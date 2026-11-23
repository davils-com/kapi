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

package com.davils.kapi.codegen.core.asyncapi.read

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference

private const val REFERENCE: String = "\$ref"

private const val COMPONENTS: String = "components"

private const val SCHEMAS: String = "schemas"

private const val MOST_HOPS: Int = 32

private const val HEX_RADIX: Int = 16

private const val ESCAPE_WIDTH: Int = 3

private const val PERCENT: Char = '%'

internal class Targets(private val document: Document) {
    val origin: DocumentUri? = document.location?.document

    fun channel(key: String): Target? = origin?.let { uri -> Target(uri, JsonPointer.of("channels", key)) }

    fun componentKey(target: Target): String? {
        val tokens = target.pointer.tokens
        val isLocal = target.document == origin && tokens.size == COMPONENT_DEPTH
        val isComponent = isLocal && tokens[0] == COMPONENTS && tokens[1] == SCHEMAS
        return tokens.last().takeIf { isComponent }
    }

    fun schemaAt(target: Target): Schema? {
        val key = componentKey(target)
        val schemas = document.components?.schemas.orEmpty()
        val component = key?.let(schemas::get)
        val declared = component?.let { referable -> document.resolve(referable) }
        return declared ?: document.targets[target] as? Schema
    }

    fun targetOf(referable: Referable<Schema>): Target? = when (referable) {
        is Referable.Referenced -> referable.reference.target
        is Referable.Inline -> referable.value.location?.let { location -> Target(location.document, location.pointer) }
    }

    fun referenceOf(value: Value): String? {
        val mapping = value as? Value.Mapping
        return (mapping?.get(REFERENCE) as? Value.Text)?.value
    }

    fun targetOf(text: String, base: DocumentUri): Target? {
        val reference = UriReference.parseOrNull(text)
        val pointer = reference?.let { parsed -> decoded(parsed.fragment.orEmpty()) }?.let(JsonPointer::parseOrNull)
        val uri = reference?.let { parsed -> documentOf(base, parsed) }
        if (pointer == null || uri == null) return null
        return Target(uri, pointer)
    }

    fun resolved(value: Value?): Value? {
        var current = value
        var hops = 0
        var text = current?.let(::referenceOf)
        while (text != null && hops < MOST_HOPS) {
            val base = current?.location?.document ?: origin
            val target = base?.let { uri -> targetOf(text, uri) }
            current = target?.let(::schemaAt)?.definition
            text = current?.let(::referenceOf)
            hops += 1
        }
        return current?.takeIf { text == null }
    }

    private fun documentOf(base: DocumentUri, reference: UriReference): DocumentUri? {
        val isUnrooted = reference.scheme == null && reference.authority == null
        val isSameDocument = isUnrooted && reference.path.isEmpty() && reference.query == null
        if (isSameDocument) return base
        val parsed = UriReference.parseOrNull(base.value)
        val resolved = parsed?.resolve(reference)?.normalize()
        return resolved?.let { uri -> DocumentUri.parseOrNull(uri.toString().substringBefore('#')) }
    }

    private fun decoded(fragment: String): String? {
        val bytes = ArrayList<Byte>(fragment.length)
        var index = 0
        var isValid = true
        while (index < fragment.length && isValid) {
            val isEscape = fragment[index] == PERCENT
            val octet = octetAt(fragment, index, isEscape)
            isValid = octet != null
            octet?.let { found -> bytes.add(found.toByte()) }
            index += ESCAPE_WIDTH.takeIf { isEscape } ?: 1
        }
        return bytes.takeIf { isValid }?.let { found -> strict(found.toByteArray()) }
    }

    private fun octetAt(fragment: String, index: Int, isEscape: Boolean): Int? {
        if (!isEscape) return fragment[index].code
        val end = minOf(a = index + ESCAPE_WIDTH, b = fragment.length)
        val escape = fragment.substring(startIndex = index + 1, endIndex = end)
        return escape.takeIf { text -> text.length == 2 }?.toIntOrNull(HEX_RADIX)
    }

    private fun strict(bytes: ByteArray): String? = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        null
    }

    private companion object {
        const val COMPONENT_DEPTH: Int = 3
    }
}
