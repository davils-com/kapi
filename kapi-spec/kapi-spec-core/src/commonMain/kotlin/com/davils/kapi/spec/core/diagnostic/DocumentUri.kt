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

package com.davils.kapi.spec.core.diagnostic

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.uri.UriReference
import kotlin.jvm.JvmInline

/**
 * The absolute URI that names a document, such as `file:///api/petstore.yaml` or `https://example.com/api.yaml`.
 *
 * A document URI is an absolute URI by RFC 3986 without a fragment: the fragment of a reference names a place inside a
 * document, never the document. Its text is checked by the strict URI grammar of Kore, which refuses spaces, non-ASCII
 * characters and control characters, so a document URI is safe to show as it is.
 *
 * @property value the text of the URI.
 * @since 1.0.0
 */
@JvmInline
public value class DocumentUri(public val value: String) : Comparable<DocumentUri> {
    init {
        require(isDocumentUri(value)) { "${SafeText.quoted(value)} is no document uri such as file:///api/a.yaml" }
    }

    /**
     * The scheme of the URI, such as `file` or `https`, as it was written.
     *
     * @since 1.0.0
     */
    public val scheme: String get() = value.substringBefore(':')

    override fun toString(): String = value

    /**
     * Compares the URIs by their text, so that document URIs can key an ordered map.
     *
     * @param other the other URI.
     * @return a negative number, zero or a positive number as this URI sorts before, with or after [other].
     * @since 1.0.0
     */
    override fun compareTo(other: DocumentUri): Int = value.compareTo(other.value)

    /**
     * Creates and reads document URIs.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The URI of a document that was handed over as text and has no place of its own. A relative reference in
         * such a document resolves against nothing and is reported.
         *
         * @since 1.0.0
         */
        public val MEMORY: DocumentUri = DocumentUri("memory:/document")

        /**
         * Reads [text] as a document URI.
         *
         * @param text the text, which may contain anything.
         * @return the document URI, or `null` if [text] is no absolute URI or has a fragment.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): DocumentUri? {
            if (!isDocumentUri(text)) return null
            return DocumentUri(text)
        }

        private fun isDocumentUri(text: String): Boolean {
            val uri = UriReference.parseOrNull(text) ?: return false
            return uri.isAbsolute && uri.fragment == null
        }
    }
}
