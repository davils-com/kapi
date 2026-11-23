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

package com.davils.kapi.runtime.client.internal

import com.davils.kapi.runtime.client.channel.ChannelClose
import com.davils.kapi.runtime.client.channel.ChannelReply
import com.davils.kapi.runtime.client.channel.ChannelSpec
import com.davils.kapi.runtime.client.channel.CloseKind
import com.davils.kapi.runtime.core.codec.Codec
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.failure.contained
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration

private const val TEXT: String = "text"

private const val JSON: String = "json"

private const val MOST_REASON_BYTES: Int = 123

private const val FIRST_APPLICATION_CODE: Int = 3_000

private const val LAST_APPLICATION_CODE: Int = 4_999

@OptIn(DavilsInternal::class)
internal class ChannelEngine<S, R : Any>(
    private val session: WebSocketSession,
    private val spec: ChannelSpec<S, R>,
    private val codec: Codec,
) {
    private val settings = spec.settings

    private val received: Channel<R> = Channel(settings.incomingCapacity)

    private val lock: Mutex = Mutex()

    private val pending: OrderedMap<String, CompletableDeferred<R?>> = OrderedMap()

    private val closing: CompletableDeferred<ChannelClose> = CompletableDeferred()

    private val assembled: Buffer = Buffer()

    private val isText: Boolean =
        settings.mediaType.type == TEXT || settings.mediaType.subtype == JSON || settings.mediaType.suffix == JSON

    val incoming: Flow<R> = received.receiveAsFlow()

    val isClosed: Boolean get() = closing.isCompleted

    suspend fun read() {
        var close: ChannelClose? = null
        try {
            var frame = session.incoming.receiveCatching().getOrNull()
            while (frame != null && close == null) {
                close = handled(frame)
                if (close == null) frame = session.incoming.receiveCatching().getOrNull()
            }
        } finally {
            withContext(NonCancellable) { ended(close) }
        }
    }

    suspend fun closed(): ChannelClose = closing.await()

    suspend fun send(message: S): Boolean {
        val encoded = contained { codec.encode(spec.sendSerializer, message) }.getOrNull() ?: return false
        return sent(encoded)
    }

    private suspend fun sent(encoded: ByteString): Boolean {
        val bytes = encoded.toByteArray()
        val frame = when (isText) {
            true -> Frame.Text(fin = true, data = bytes)
            false -> Frame.Binary(fin = true, data = bytes)
        }
        return !isClosed && attempted { session.outgoing.send(frame) }.isSuccess
    }

    suspend fun request(message: S, timeout: Duration): ChannelReply<R> {
        val encoded = contained { codec.encode(spec.sendSerializer, message) }.getOrNull()
        val id = sentId(message, encoded)
        val waiter = id?.let { found -> registered(found) } ?: return ChannelReply.Refused
        val isSent = encoded != null && sent(encoded)
        if (!isSent) waiter.complete(null)
        val waited = withTimeoutOrNull(timeout) { Result.success(waiter.await()) }
        lock.withLock { pending.remove(id) }
        val reply = waited?.getOrNull()?.let { value -> ChannelReply.Received(value) }
        val unanswered = ChannelReply.TimedOut.takeIf { waited == null } ?: ChannelReply.Closed
        return reply ?: unanswered
    }

    suspend fun close(code: Int, reason: String) {
        val isCode = code == CloseKind.Normal.code || code in FIRST_APPLICATION_CODE..LAST_APPLICATION_CODE
        require(isCode && reason.encodeToByteArray().size <= MOST_REASON_BYTES) {
            "A close names 1000 or a code from 3000 to 4999 and a reason of at most 123 bytes"
        }
        closeWith(ChannelClose(code = code, reason = reason, isLocal = true))
    }

    private suspend fun sessionClose(): ChannelClose? {
        val owned = session as? DefaultWebSocketSession ?: return null
        val reason = withTimeoutOrNull(settings.closeTimeout) { owned.closeReason.await() }
        return reason?.let { found -> ChannelClose(code = found.code.toInt(), reason = found.message) }
    }

    private suspend fun registered(id: String): CompletableDeferred<R?>? = lock.withLock {
        val isFree = id !in pending && pending.size < settings.maxPendingRequests && !isClosed
        val waiter = CompletableDeferred<R?>().takeIf { isFree }
        waiter?.let { found -> pending[id] = found }
        waiter
    }

    private suspend fun handled(frame: Frame): ChannelClose? = when (frame) {
        is Frame.Text -> data(frame)
        is Frame.Binary -> data(frame)
        is Frame.Close -> peerClose(frame)
        is Frame.Ping -> null
        is Frame.Pong -> null
    }

    private fun peerClose(frame: Frame.Close): ChannelClose {
        val reason = frame.readReason() ?: return ChannelClose(code = CloseKind.NoStatus.code)
        return ChannelClose(code = reason.code.toInt(), reason = reason.message)
    }

    private suspend fun data(frame: Frame): ChannelClose? {
        val isFrameTooBig = frame.data.size > settings.maxFrameBytes
        if (!isFrameTooBig) assembled.write(frame.data)
        val isTooBig = isFrameTooBig || assembled.size > settings.maxMessageBytes
        if (isTooBig) return closeWith(ChannelClose(code = CloseKind.TooBig.code, isLocal = true))
        return frame.takeIf { found -> found.fin }?.let { delivered(assembled.readByteArray()) }
    }

    private suspend fun delivered(bytes: ByteArray): ChannelClose? =
        when (val decoding = codec.decode(spec.receiveDeserializer, ByteString(bytes))) {
            is Decoding.Decoded -> {
                routed(decoding.value, ByteString(bytes))
                null
            }

            is Decoding.Failed -> {
                closeWith(ChannelClose(code = CloseKind.InvalidPayload.code, isLocal = true))
            }
        }

    private fun sentId(message: S, encoded: ByteString?): String? {
        val correlation = spec.correlation ?: return null
        val sentJson = correlation.json?.sent
        val found = contained { correlation.sent(message) }.getOrNull()
        val typed = found.takeIf { sentJson == null }
        return typed ?: encoded?.let { bytes -> jsonId(bytes, sentJson) }
    }

    private fun receivedId(message: R, bytes: ByteString): String? {
        val correlation = spec.correlation ?: return null
        val receivedJson = correlation.json?.received
        val found = contained { correlation.received(message) }.getOrNull()
        val typed = found.takeIf { receivedJson == null }
        return typed ?: jsonId(bytes, receivedJson)
    }

    private fun jsonId(bytes: ByteString, id: ((JsonElement) -> String?)?): String? {
        val decoded = (codec.decode(JsonElement.serializer(), bytes) as? Decoding.Decoded)?.value
        val element = decoded?.takeIf { id != null } ?: return null
        return contained { id?.invoke(element) }.getOrNull()
    }

    private suspend fun routed(message: R, bytes: ByteString) {
        val id = receivedId(message, bytes)
        val waiter = id?.let { found -> lock.withLock { pending.remove(found) } }
        if (waiter == null) received.send(message)
        waiter?.complete(message)
    }

    private suspend fun closeWith(close: ChannelClose): ChannelClose {
        val reason = CloseReason(code = close.code.toShort(), message = close.reason)
        if (!isClosed) session.close(reason)
        closing.complete(close)
        return close
    }

    private suspend fun ended(close: ChannelClose?) {
        val reason = close ?: closing.takeIf(CompletableDeferred<ChannelClose>::isCompleted)?.await() ?: sessionClose()
        closing.complete(reason ?: ChannelClose(code = CloseKind.Abnormal.code))
        received.close()
        val waiting = lock.withLock {
            val all = pending.entries.map { (_, waiter) -> waiter }
            pending.clear()
            all
        }
        waiting.forEach { waiter -> waiter.complete(null) }
    }
}
