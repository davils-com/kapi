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

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.schema.DiagnosticCodes
import com.davils.kapi.spec.schema.SchemaLimits
import com.davils.kapi.spec.schema.internal.Uris
import com.davils.kapi.spec.schema.internal.dialect.Profile
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kapi.spec.schema.internal.dialect.Profiles
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.collection.OrderedSet
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.violation.Severity
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal class Session(
    val limits: SchemaLimits,
    val defaultProfile: Profile,
    private val formatAssertion: Boolean,
) {
    val documents: MutableList<Document> = ArrayList()

    val resources: MutableList<Resource> = ArrayList()

    val diagnostics: MutableList<Diagnostic> = ArrayList()

    val references: MutableList<PendingReference> = ArrayList()

    val locations: OrderedMap<String, LocationEntry> = OrderedMap()

    val metaProfiles: OrderedMap<String, Profile> = OrderedMap()

    val failedMetaSchemas: OrderedSet<String> = OrderedSet()

    private val resourcesByUri = OrderedMap<String, Int>()

    private val resourcesByLocation = OrderedMap<String, Int>()

    private val documentsByUri = OrderedMap<String, Int>()

    private val anchors = OrderedMap<String, Target>()

    var isStrict: Boolean = true

    fun documentOf(uri: String): Document? = documentsByUri[uri]?.let(documents::get)

    fun addDocument(uri: String, root: JsonElement): Document {
        if (documents.size >= limits.maximumDocuments) {
            abort(DiagnosticCodes.LIMIT_EXCEEDED, "The schema needs more than ${limits.maximumDocuments} documents", null)
        }
        val location = DocumentUri.parseOrNull(uri) ?: DocumentUri.MEMORY
        val document = Document(documents.size, uri, root, location)
        documents += document
        documentsByUri[uri] = document.index
        resourceFor(uri, Target(document.index, emptyList()))
        return document
    }

    fun resourceFor(uri: String, target: Target): Int {
        val locationKey = keyOf(target)
        val byUri = resourcesByUri[uri]
        val byLocation = resourcesByLocation[locationKey]
        val isConflict = byUri != null && byUri != byLocation
        if (isConflict && isStrict) {
            report(DiagnosticCodes.ID_DUPLICATE, "The identifier ${SafeText.quoted(uri)} names two schemas", target)
        }
        return byUri ?: aliasOrCreate(uri, target, byLocation)
    }

    private fun aliasOrCreate(uri: String, target: Target, existing: Int?): Int {
        val index = existing ?: resources.size
        val resource = existing?.let(resources::get) ?: Resource(index, uri, target.document, target.pointer)
        if (existing == null) resources += resource
        resource.uri = uri
        resourcesByUri[uri] = index
        resourcesByLocation[keyOf(target)] = index
        return index
    }

    fun resourceIndexOf(uri: String): Int? = resourcesByUri[uri]

    fun addAnchor(resource: Int, name: String, target: Target) {
        val key = resources[resource].uri + "#" + name
        val existing = anchors[key]
        val isConflict = existing != null && keyOf(existing) != keyOf(target)
        if (isConflict && isStrict) {
            report(DiagnosticCodes.ID_DUPLICATE, "The anchor ${SafeText.quoted(name)} names two schemas", target)
        }
        if (existing == null) anchors[key] = target
    }

    fun record(target: Target, resource: Int, profile: Profile) {
        locations[keyOf(target)] = LocationEntry(target, resource, profile)
    }

    fun locationOf(target: Target): LocationEntry? = locations[keyOf(target)]

    fun profileFor(uri: String): Profile? {
        val dialect = Profiles.dialectOf(uri)
        if (dialect != null) return Profiles.of(dialect, formatAssertion)
        val key = Uris.normalized(uri) ?: uri
        return metaProfiles[key] ?: defaultProfile.takeIf { key in failedMetaSchemas }
    }

    fun locate(absolute: String): Target? {
        val uri = Uris.withoutFragment(absolute)
        val resource = resourcesByUri[uri]?.let(resources::get) ?: return null
        return targetIn(resource, Uris.fragmentOf(absolute))
    }

    private fun targetIn(resource: Resource, fragment: String): Target? {
        if (fragment.isEmpty()) return Target(resource.document, resource.pointer)
        val isPointer = fragment.startsWith("/")
        val tokens = Uris.pointerOf(fragment).takeIf { isPointer }
        val pointed = tokens?.let { Target(resource.document, resource.pointer + it) }
        val existing = pointed?.takeIf { elementAt(it) != null }
        return existing ?: anchors[resource.uri + "#" + fragment].takeIf { !isPointer }
    }

    fun elementAt(target: Target): JsonElement? {
        val root: JsonElement? = documents[target.document].root
        return target.pointer.fold(root) { element, token -> childOf(element, token) }
    }

    private fun childOf(element: JsonElement?, token: String): JsonElement? {
        val member = (element as? JsonObject)?.get(token)
        return member ?: (element as? JsonArray)?.let { array -> indexOf(token)?.let(array::getOrNull) }
    }

    private fun indexOf(token: String): Int? {
        val isCanonical = token.isNotEmpty() && token.all { it in '0'..'9' } && (token == "0" || !token.startsWith('0'))
        return token.takeIf { isCanonical }?.toIntOrNull()
    }

    fun absoluteOf(resource: Int, pointer: List<String>): String {
        val owner = resources[resource]
        val isInside = pointer.size >= owner.pointer.size && pointer.subList(0, owner.pointer.size) == owner.pointer
        val relative = pointer.drop(owner.pointer.size).takeIf { isInside } ?: pointer
        return owner.uri + "#" + JsonPointer.of(relative).text
    }

    fun report(code: ViolationCode, message: String, target: Target?) {
        if (diagnostics.size >= limits.maximumErrors) {
            throw CompileAbort()
        }
        val document = target?.let { documents[it.document].location } ?: DocumentUri.MEMORY
        val pointer = JsonPointer.of(target?.pointer.orEmpty())
        diagnostics += Diagnostic(code, Severity.Error, message, Location(document, pointer))
    }

    fun abort(code: ViolationCode, message: String, target: Target?): Nothing {
        report(code, message, target)
        throw CompileAbort()
    }

    companion object {
        fun keyOf(target: Target): String = target.document.toString() + ":" + JsonPointer.of(target.pointer).text
    }
}
