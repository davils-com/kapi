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

package com.davils.kapi.spec.asyncapi.read.reference

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.model.Target
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference

private const val HEX_RADIX: Int = 16

private const val PERCENT: Char = '%'

private const val ESCAPE_WIDTH: Int = 3

private const val ASCII_END: Int = 0x80

internal object UriResolution {
    fun parse(text: String): UriReference? = UriReference.parseOrNull(text)

    fun documentOf(base: DocumentUri, reference: UriReference): DocumentUri? {
        val isSameDocument = reference.scheme == null &&
            reference.authority == null &&
            reference.path.isEmpty() &&
            reference.query == null
        if (isSameDocument) return base
        val hierarchical = parse(
            base.value,
        )?.takeIf { parsed -> parsed.authority != null || parsed.path.startsWith('/') }
        val normalized = hierarchical?.resolve(reference)?.normalize()
        return normalized?.let { resolved -> DocumentUri.parseOrNull(resolved.toString().substringBefore('#')) }
    }

    fun pointerOf(reference: UriReference): JsonPointer? {
        val decoded = decode(reference.fragment.orEmpty()) ?: return null
        return JsonPointer.parseOrNull(decoded)
    }

    fun target(base: DocumentUri, reference: UriReference): Target? {
        val document = documentOf(base, reference)
        val pointer = pointerOf(reference)
        if (document == null || pointer == null) return null
        return Target(document, pointer)
    }

    private fun decode(fragment: String): String? {
        val bytes = ArrayList<Byte>(fragment.length)
        var index = 0
        while (index < fragment.length) {
            val isEscape = fragment[index] == PERCENT
            val octet = octetAt(fragment, index, isEscape) ?: return null
            bytes.add(octet.toByte())
            index += ESCAPE_WIDTH.takeIf { isEscape } ?: 1
        }
        return strict(bytes.toByteArray())
    }

    private fun octetAt(fragment: String, index: Int, isEscape: Boolean): Int? {
        if (!isEscape) return fragment[index].code.takeIf { code -> code < ASCII_END }
        val high = fragment.getOrNull(index + 1)?.digitToIntOrNull(HEX_RADIX)
        val low = fragment.getOrNull(index + 2)?.digitToIntOrNull(HEX_RADIX)
        val octet = listOfNotNull(high, low).takeIf { halves -> halves.size == 2 }
        return octet?.let { (upper, lower) -> upper * HEX_RADIX + lower }
    }

    private fun strict(bytes: ByteArray): String? = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        null
    }
}
