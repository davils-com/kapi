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
import kotlinx.coroutines.flow.Flow
import kotlinx.io.bytestring.ByteString

/**
 * The content of a request that is streamed from a flow, such as a file larger than the application should hold in
 * memory.
 *
 * An upload is collected once, so a call that carries one is sent once: it is neither retried, nor sent again after a
 * refreshed credential, nor followed through a redirect that keeps the method. [toString] never shows the content.
 *
 * @property contentType the media type of the content.
 * @property chunks the content in the order it is sent.
 * @property length the number of bytes, or `null` to send the content in chunks.
 * @since 1.0.0
 */
public class Upload(
    public val contentType: MediaType,
    public val chunks: Flow<ByteString>,
    public val length: Long? = null,
) {
    override fun toString(): String = "Upload($contentType, length=${length ?: "unknown"})"
}
