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

package com.davils.kapi.spec.core.model

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference

private const val HEX_RADIX: Int = 16

private const val PERCENT: Char = '%'

private const val ESCAPE_WIDTH: Int = 3

/**
 * Resolves the URI references of API documents by RFC 3986 against the document that holds them: the document they
 * name and the JSON Pointer of their fragment, percent-decoded as strict UTF-8.
 *
 * Shared by the readers of kapi and by its generator. It reads no document and touches no network; a reference that
 * names no place, such as one against a base that is no hierarchical URI or a fragment that is no JSON Pointer,
 * resolves to `null`. It is stateless and safe to use from any thread.
 *
 * @since 1.0.0
 */
@DavilsInternal
public object UriResolution {
    /**
     * Parses [text] as a URI reference of RFC 3986.
     *
     * @param text the reference as written, which may be any text.
     * @return the reference, or `null` if [text] is none, such as one with a percent sign that starts no octet.
     * @since 1.0.0
     */
    public fun parse(text: String): UriReference? = UriReference.parseOrNull(text)

    /**
     * Returns the document [reference] names, resolved against [base] without its fragment.
     *
     * @param base the document that holds the reference.
     * @param reference the reference.
     * @return the document, [base] itself for a reference of a fragment only, or `null` if [base] is no
     * hierarchical URI a relative reference could be resolved against.
     * @since 1.0.0
     */
    public fun documentOf(base: DocumentUri, reference: UriReference): DocumentUri? {
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

    /**
     * Returns the URI a document declares for itself, such as with `${'$'}self` of OpenAPI 3.2, resolved against the
     * URI it was retrieved from.
     *
     * @param retrieval the URI the document was retrieved from.
     * @param text the declared URI as written, which may be any text.
     * @return the URI, or `null` if [text] is no URI reference, has a fragment or cannot be resolved.
     * @since 1.0.0
     */
    public fun selfOf(retrieval: DocumentUri, text: String): DocumentUri? {
        val reference = parse(text)?.takeIf { parsed -> parsed.fragment == null }
        val relative = reference?.takeUnless(UriReference::isAbsolute)
        val absolute = reference?.takeIf(UriReference::isAbsolute)
        val resolved = relative?.let { parsed -> documentOf(retrieval, parsed) }
        return resolved ?: absolute?.let { parsed -> DocumentUri.parseOrNull(parsed.normalize().toString()) }
    }

    /**
     * Returns the JSON Pointer of the fragment of [reference], percent-decoded.
     *
     * @param reference the reference.
     * @return the pointer, the root for no fragment, or `null` if the fragment decodes to no UTF-8 or no pointer.
     * @since 1.0.0
     */
    public fun pointerOf(reference: UriReference): JsonPointer? {
        val decoded = decode(reference.fragment.orEmpty()) ?: return null
        return JsonPointer.parseOrNull(decoded)
    }

    /**
     * Returns the place [reference] names: its document resolved against [base] and the pointer of its fragment.
     *
     * @param base the document that holds the reference.
     * @param reference the reference.
     * @return the place, or `null` if the document or the pointer cannot be resolved.
     * @since 1.0.0
     */
    public fun target(base: DocumentUri, reference: UriReference): Target? {
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
            val width = ESCAPE_WIDTH.takeIf { isEscape } ?: 1
            bytes.add(octetAt(fragment, index, isEscape).toByte())
            index += width
        }
        return try {
            bytes.toByteArray().decodeToString(throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun octetAt(fragment: String, index: Int, isEscape: Boolean): Int {
        if (!isEscape) return fragment[index].code
        return fragment.substring(startIndex = index + 1, endIndex = index + ESCAPE_WIDTH).toInt(HEX_RADIX)
    }
}
