/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.schema.internal.compile

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.schema.Compilation
import com.davils.kapi.spec.schema.CompiledSchema
import com.davils.kapi.spec.schema.Compiler
import com.davils.kapi.spec.schema.DiagnosticCodes
import com.davils.kapi.spec.schema.internal.Uris
import com.davils.kapi.spec.schema.internal.dialect.Profile
import com.davils.kapi.spec.schema.internal.dialect.Profiles
import com.davils.kapi.spec.schema.internal.meta.MetaSchemas
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal class Driver(private val compiler: Compiler) {
    private val session = Session(
        limits = compiler.limits,
        defaultProfile = Profiles.of(compiler.defaultDialect, compiler.isFormatAsserted),
        isFormatAsserted = compiler.isFormatAsserted,
    )

    private val requested = OrderedSet<String>()

    private val resolving = OrderedSet<String>()

    suspend fun compile(schema: JsonElement, uri: DocumentUri?): Compilation {
        val built = try {
            build(schema, uri)
        } catch (_: CompileAbort) {
            null
        }
        val isValid = built != null && session.diagnostics.isEmpty()
        if (!isValid) return Compilation.Invalid(session.diagnostics.toList())
        return Compilation.Compiled(built)
    }

    private suspend fun build(schema: JsonElement, uri: DocumentUri?): CompiledSchema {
        val retrieval = uri?.let { Uris.normalized(it.value) } ?: DocumentUri.MEMORY.value
        val document = admit(retrieval, schema, session.defaultProfile)
        preload()
        val factory = NodeFactory(session)
        val root = factory.nodeAt(document.index, emptyList())
        factory.compileAll()
        val graph = factory.graph()
        val base = graph.resources[graph.nodes[root].resource].uri
        return CompiledSchema(graph = graph, root = root, limits = compiler.limits, uri = base)
    }

    private suspend fun admit(uri: String, json: JsonElement, profile: Profile): Document {
        val document = session.documentOf(uri) ?: session.addDocument(uri, json)
        var missing = scanned(document, profile)
        while (missing != null) {
            resolveMeta(missing, document)
            missing = scanned(document, profile)
        }
        return document
    }

    private fun scanned(document: Document, profile: Profile): MetaSchemaNeeded? {
        val diagnostics = session.diagnostics.size
        val references = session.references.size
        val scanner = Scanner(session, document)
        val scan = runCatching {
            scanner.scan(
                element = document.root,
                pointer = emptyList(),
                resource = session.rootResourceOf(document),
                profile = profile,
                isRoot = true,
            )
        }
        val missing = scan.exceptionOrNull()
        missing?.takeIf { it !is MetaSchemaNeeded }?.let { throw it }
        if (missing != null) {
            truncate(session.diagnostics, diagnostics)
            truncate(session.references, references)
        }
        return missing as? MetaSchemaNeeded
    }

    private fun <T> truncate(list: MutableList<T>, size: Int) {
        while (list.size > size) list.removeAt(list.size - 1)
    }

    private suspend fun resolveMeta(missing: MetaSchemaNeeded, document: Document) {
        val key = Uris.normalized(missing.uri) ?: missing.uri
        val isCircular = key in resolving
        val json = fetch(key).takeIf { !isCircular }
        if (json == null) {
            val message = "The meta-schema ${SafeText.quoted(missing.uri)} is unknown and could not be loaded"
            session.report(DiagnosticCodes.DIALECT_UNKNOWN, message, Target(document.index, missing.pointer))
            session.failedMetaSchemas.add(key)
            return
        }
        resolving.add(key)
        val meta = admit(key, json, session.defaultProfile)
        resolving.remove(key)
        val metaProfile = session.locationOf(Target(meta.index, emptyList()))?.profile ?: session.defaultProfile
        session.metaProfiles[key] = Profiles.custom(json, metaProfile, compiler.isFormatAsserted) { vocabulary ->
            val message = "The meta-schema requires the unknown vocabulary ${SafeText.quoted(vocabulary)}"
            session.report(DiagnosticCodes.VOCABULARY_UNKNOWN, message, Target(document.index, missing.pointer))
        }
    }

    private suspend fun preload() {
        var next = 0
        while (next < session.references.size) {
            val reference = session.references[next]
            next++
            val isKnown = session.resourceIndexOf(reference.uri) != null || reference.uri in requested
            requested.add(reference.uri)
            val json = reference.uri.takeIf { !isKnown }?.let { fetch(it) }
            json?.let { admit(reference.uri, it, reference.profile) }
        }
    }

    private suspend fun fetch(uri: String): JsonElement? = local(uri) ?: remote(uri)

    private fun local(uri: String): JsonElement? {
        val registered = compiler.registry[uri]
        return registered ?: MetaSchemas.textOf(uri)?.let(Json::parseToJsonElement)
    }

    private suspend fun remote(uri: String): JsonElement? {
        val loader = compiler.loader
        val documentUri = DocumentUri.parseOrNull(uri)
        if (loader == null || documentUri == null) return null
        val result = attempted { loader.load(documentUri) }
        if (result.isFailure) {
            val message = "The loader failed to load ${SafeText.quoted(uri)}"
            session.report(DiagnosticCodes.LOADER_FAILED, message, null)
        }
        return result.getOrNull()
    }
}
