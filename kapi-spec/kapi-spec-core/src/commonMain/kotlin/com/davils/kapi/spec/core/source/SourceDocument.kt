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
import kotlinx.io.bytestring.ByteString

/**
 * A document a reading loaded: where it came from, how long it was and the SHA-256 digest of its bytes, so a build
 * can record exactly which documents it read.
 *
 * @property uri the URI of the document.
 * @property length the number of bytes of the document.
 * @property digest the SHA-256 digest of the bytes of the document.
 * @since 1.0.0
 */
public class SourceDocument(
    public val uri: DocumentUri,
    public val length: Int,
    public val digest: ByteString,
) {
    override fun equals(other: Any?): Boolean =
        other is SourceDocument && uri == other.uri && length == other.length && digest == other.digest

    override fun hashCode(): Int = listOf(uri, length, digest).hashCode()

    override fun toString(): String = "SourceDocument($uri, $length bytes)"
}
