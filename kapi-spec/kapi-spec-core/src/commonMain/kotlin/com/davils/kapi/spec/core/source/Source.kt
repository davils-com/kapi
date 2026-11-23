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

package com.davils.kapi.spec.core.source

import com.davils.kapi.spec.core.diagnostic.DocumentUri

/**
 * Where a reading starts: a document handed over as text, or one a [Resolver] loads from its URI.
 *
 * @since 1.0.0
 */
public sealed interface Source {
    /**
     * The URI of the document, against which its relative references resolve.
     *
     * @since 1.0.0
     */
    public val uri: DocumentUri

    /**
     * A document handed over as text.
     *
     * Its text is shown nowhere: [toString] names only its URI and its length.
     *
     * @property text the text of the document.
     * @property uri the URI of the document; [DocumentUri.MEMORY] when it has no place of its own.
     * @since 1.0.0
     */
    public class Text(
        public val text: String,
        override val uri: DocumentUri = DocumentUri.MEMORY,
    ) : Source {
        override fun equals(other: Any?): Boolean = other is Text && text == other.text && uri == other.uri

        override fun hashCode(): Int = 31 * text.hashCode() + uri.hashCode()

        override fun toString(): String = "Text($uri, ${text.length} characters)"
    }

    /**
     * A document the [Resolver] of the reading loads from its URI.
     *
     * @property uri the URI of the document.
     * @since 1.0.0
     */
    public class Located(override val uri: DocumentUri) : Source {
        override fun equals(other: Any?): Boolean = other is Located && uri == other.uri

        override fun hashCode(): Int = uri.hashCode()

        override fun toString(): String = "Located($uri)"
    }
}
