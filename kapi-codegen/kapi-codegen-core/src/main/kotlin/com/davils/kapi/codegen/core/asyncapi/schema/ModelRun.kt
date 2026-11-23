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

package com.davils.kapi.codegen.core.asyncapi.schema

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.asyncapi.GeneratorOptions
import com.davils.kapi.codegen.core.asyncapi.Report
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import com.squareup.kotlinpoet.CodeBlock

private const val COMPONENT_DEPTH: Int = 3

private const val QUOTE: String = "\""

private val componentPath: List<String> = listOf("components", "schemas")

internal class ModelRun(private val options: GeneratorOptions, private val report: Report) {
    suspend fun run(document: Document, hoisted: List<Hoisted>): Models? {
        val root = document.location
        val uri = root?.document ?: DocumentUri.MEMORY
        val byKey = hoisted.associateBy(Hoisted::key)
        val text = JsonText.document(options.apiName, hoisted)
        val reading = OpenApi.reader().read(Source.Text(text, uri))
        reading.diagnostics.forEach { diagnostic -> remapped(diagnostic, byKey, root) }
        val read = reading as? Reading.Read
        val generation = read?.let { found -> generator().generate(found.document) }
        generation?.run { diagnostics.forEach { diagnostic -> remapped(diagnostic, byKey, root) } }
        val generated = generation as? Generation.Generated
        return generated?.let { found -> Models(rewritten(found.files, uri, hoisted), found.catalog) }
    }

    private fun generator() = Codegen.openApi {
        packageName = options.packageName
        apiName = options.apiName
        typePrefix = options.typePrefix
        visibility = options.visibility
        generatorVersion = options.generatorVersion
        artifacts = setOf(Artifact.Models)
    }

    private fun remapped(diagnostic: Diagnostic, byKey: Map<String, Hoisted>, root: Location?) {
        val tokens = diagnostic.location?.run { pointer.tokens }.orEmpty()
        val isSchema = tokens.size >= COMPONENT_DEPTH && tokens.take(2) == componentPath
        val key = tokens.getOrNull(2)?.takeIf { isSchema }
        val hoisted = key?.let(byKey::get)
        val location = hoisted?.let { found -> located(found, tokens.drop(COMPONENT_DEPTH)) } ?: root
        val isKept = hoisted != null || diagnostic.isError
        val moved = Diagnostic(
            code = diagnostic.code,
            severity = diagnostic.severity,
            message = diagnostic.message,
            location = location,
        )
        if (isKept) report.add(moved)
    }

    private fun located(hoisted: Hoisted, rest: List<String>): Location? {
        var current: Value? = hoisted.schema.definition
        var deepest: Location? = current?.location ?: hoisted.schema.location
        rest.forEach { token ->
            current = childOf(current, token)
            current?.location?.let { found -> deepest = found }
        }
        return deepest
    }

    private fun childOf(value: Value?, token: String): Value? {
        val mapping = value as? Value.Mapping
        val sequence = value as? Value.Sequence
        val items = sequence?.items.orEmpty()
        return mapping?.get(token) ?: token.toIntOrNull()?.let(items::getOrNull)
    }

    private fun rewritten(files: List<GeneratedFile>, uri: DocumentUri, hoisted: List<Hoisted>): List<GeneratedFile> {
        val name = uri.value.substringAfterLast('/')
        val moves = hoisted.flatMap { found -> movesOf(name, uri, found) }
        val kotlin = files.filter { file -> file.kind == GeneratedFileKind.Kotlin }
        return kotlin.map { file ->
            val content = moves.fold(file.content) { text, (from, to) -> text.replace(oldValue = from, newValue = to) }
            GeneratedFile(path = file.path, kind = file.kind, content = content, platform = file.platform)
        }
    }

    private fun movesOf(name: String, uri: DocumentUri, hoisted: Hoisted): List<Pair<String, String>> {
        val synthetic = JsonPointer.of(componentPath + hoisted.key)
        val isMoved = hoisted.origin.document != uri || hoisted.origin.pointer != synthetic
        val document = hoisted.origin.document.value
        val origin = document.substringAfterLast('/')
        val from = literal(name + "#" + synthetic.text)
        val to = literal(origin + "#" + hoisted.origin.pointer.text)
        val ends = listOf(QUOTE, "/").filter { isMoved }
        return ends.map { end -> from + end to to + end }
    }

    private fun literal(text: String): String {
        val code = CodeBlock.of("%S", text)
        return code.toString().removeSuffix(QUOTE)
    }
}
