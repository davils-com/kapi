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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.response.HttpStatus
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.ReplyBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.utils.io.ByteWriteChannel

internal class ReplyWriter(private val codecs: Codecs) {
    fun encoded(reply: Reply): Reply {
        val body = reply.body as? ReplyBody.Value ?: return reply
        return Reply(reply.status, reply.headers, ReplyBody.Bytes(body.mediaType, body.encode(codecs)))
    }

    suspend fun write(call: ApplicationCall, reply: Reply) {
        reply.headers.forEach { (name, value) -> call.response.headers.append(name, value, safeOnly = false) }
        val status = HttpStatusCode.fromValue(reply.status)
        val isEmpty = reply.status == HttpStatus.NO_CONTENT || reply.status == HttpStatus.NOT_MODIFIED
        when (val body = reply.body.takeUnless { isEmpty } ?: ReplyBody.Empty) {
            ReplyBody.Empty -> call.respond(Empty(status))

            is ReplyBody.Bytes -> call.respondBytes(body.content.toByteArray(), contentTypeOf(body.mediaType), status)

            is ReplyBody.Value -> call.respondBytes(
                body.encode(codecs).toByteArray(),
                contentTypeOf(body.mediaType),
                status,
            )

            is ReplyBody.Stream -> call.respond(Streamed(status, body, codecs))
        }
    }

    private class Empty(override val status: HttpStatusCode) : OutgoingContent.NoContent()

    private class Streamed(
        override val status: HttpStatusCode,
        private val body: ReplyBody.Stream,
        private val codecs: Codecs,
    ) : OutgoingContent.WriteChannelContent() {
        override val contentType: ContentType = contentTypeOf(body.mediaType)

        override val contentLength: Long? = body.length

        override suspend fun writeTo(channel: ByteWriteChannel) {
            body.write(channel, codecs)
        }
    }

    private companion object {
        fun contentTypeOf(mediaType: MediaType): ContentType = ContentType.parse(mediaType.toString())
    }
}
