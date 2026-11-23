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

package com.davils.kapi.runtime.client.call

import com.davils.kapi.runtime.core.negotiation.MediaType
import kotlinx.io.bytestring.ByteString

private const val HASH_FACTOR: Int = 31

/**
 * The content of a request that is held in memory, so it can be sent again on a retry or a redirect.
 *
 * A body is immutable and safe to share between threads. [toString] names the media type and the size, never the
 * content.
 *
 * @property contentType the media type of the content.
 * @property content the content.
 * @since 1.0.0
 */
public class Body(public val contentType: MediaType, public val content: ByteString) {
    override fun equals(other: Any?): Boolean =
        other is Body && contentType == other.contentType && content == other.content

    override fun hashCode(): Int = contentType.hashCode() * HASH_FACTOR + content.hashCode()

    override fun toString(): String = "Body($contentType, ${content.size} bytes)"
}
