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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.server.channel.ChannelEndpoint
import com.davils.kapi.runtime.server.channel.ChannelReply
import com.davils.kapi.runtime.server.channel.ChannelSession
import com.davils.kapi.runtime.server.channel.Correlation
import com.davils.kapi.runtime.server.request.Request
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.failure.contained
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.bytestring.ByteString
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

private const val OPEN: Int = -1

private const val MOST_REASON_BYTES: Int = 123

internal class TypedSession<Receive : Any, Send : Any>(
    private val session: DefaultWebSocketSession,
    override val request: Request,
    override val protocol: String?,
    private val endpoint: ChannelEndpoint<Receive, Send>,
    private val codecs: Codecs,
) : ChannelSession<Receive, Send> {
    private val closed = AtomicInt(OPEN)

    val closeCode: Int get() = closed.load()

    override val incoming: Flow<Receive> = flow {
        var message = receive()
        while (message != null) {
            emit(message)
            message = receive()
        }
    }

    private val held: ArrayDeque<Receive> = ArrayDeque()

    override suspend fun receive(): Receive? = held.removeFirstOrNull() ?: read()

    override suspend fun request(message: Send, timeout: Duration): ChannelReply<Receive> {
        val correlation = endpoint.correlation
        val id = correlation?.let { found -> contained { found.sent(message) }.getOrNull() }
        if (correlation == null || id == null) return ChannelReply.Refused
        return awaited(message = message, id = id, correlation = correlation, timeout = timeout)
    }

    private suspend fun awaited(
        message: Send,
        id: String,
        correlation: Correlation<Send, Receive>,
        timeout: Duration,
    ): ChannelReply<Receive> {
        val isSent = closeCode == OPEN && attempted { send(message) }.isSuccess
        if (!isSent) return ChannelReply.Closed
        return withTimeoutOrNull(timeout) { replyTo(id, correlation) } ?: ChannelReply.TimedOut
    }

    private suspend fun replyTo(id: String, correlation: Correlation<Send, Receive>): ChannelReply<Receive> {
        var reply: ChannelReply<Receive>? = null
        var message = read()
        while (message != null && reply == null) {
            val found = message
            val isReply = contained { correlation.received(found) }.getOrNull() == id
            if (isReply) reply = ChannelReply.Received(found)
            if (!isReply) message = held(found)
        }
        return reply ?: ChannelReply.Closed
    }

    private suspend fun held(message: Receive): Receive? {
        val isFull = held.size >= endpoint.limits.maxHeldMessages
        if (isFull) return closedWith(ChannelSession.POLICY_VIOLATION)
        held.addLast(message)
        return read()
    }

    private suspend fun read(): Receive? {
        val content = assembled() ?: return null
        val codec = endpoint.receive
        val decoding = codecs.decode(codec.serializer, content, codec.mediaType) as? Decoding.Decoded
        return decoding?.value ?: closedWith(ChannelSession.INVALID_PAYLOAD)
    }

    override suspend fun send(message: Send) {
        val codec = endpoint.send
        val writer = checkNotNull(codecs.codecFor(codec.mediaType)) { "No codec of the server writes the message" }
        val data = writer.encode(codec.serializer, message).toByteArray()
        val frame: Frame = Frame.Text(true, data).takeIf { codec.isText } ?: Frame.Binary(true, data)
        session.send(frame)
    }

    override suspend fun close(code: Short, reason: String) {
        val isReason = reason.encodeToByteArray().size <= MOST_REASON_BYTES && HeaderChecks.isValue(reason)
        require(isReason) { "A close reason has at most 123 bytes and no control character" }
        if (closed.compareAndSet(expectedValue = OPEN, newValue = code.toInt())) {
            attempted { session.close(CloseReason(code, reason)) }
        }
    }

    private suspend fun assembled(): ByteString? {
        val frame = session.incoming.receiveCatching().getOrNull() ?: return null
        val isFitting = frame.data.size <= endpoint.limits.maxMessageBytes
        return ByteString(frame.data).takeIf { isFitting } ?: closedWith(ChannelSession.TOO_BIG)
    }

    private suspend fun <T> closedWith(code: Short): T? {
        close(code, "")
        return null
    }
}
