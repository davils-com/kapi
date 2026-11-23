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

package com.davils.kapi.runtime.client.channel

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.internal.ChannelEngine
import com.davils.kapi.runtime.client.internal.ChannelOpening
import com.davils.kapi.runtime.core.codec.Codec
import com.davils.kore.core.failure.contained
import com.davils.kore.core.outcome.Outcome
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Opens the WebSocket channel of [call] by RFC 6455, runs [block] with its session and closes it.
 *
 * The call is authorized like any other: its credentials go into the headers, the query or the cookies of the
 * handshake, and its URL, `ws` and `wss` judged as `http` and `https`, passes the destination policy. The client
 * offers the subprotocols of the spec and refuses a server that chooses another. The `HttpClient` must have the
 * `WebSockets` plugin; pings run in the session of Ktor when [ChannelSettings.pingInterval] is above zero. The block
 * runs in the coroutine of the caller; when it returns, the client closes the channel with 1000, waits at most
 * [ChannelSettings.closeTimeout] for the peer and ends the session. Whatever the block throws passes on after the
 * channel closed.
 *
 * @receiver the client.
 * @param S the type of the messages the client sends.
 * @param R the type of the messages the client receives.
 * @param T the type of the value of the block.
 * @param call the call of the handshake, such as `GET /chat`.
 * @param spec the messages and settings of the channel.
 * @param block what to do with the open channel.
 * @return the value of the block with how the channel closed, or why it did not open.
 * @since 1.0.0
 */
public suspend fun <S, R : Any, T> ApiClient.channel(
    call: Call,
    spec: ChannelSpec<S, R>,
    block: suspend ChannelSession<S, R>.() -> T,
): ChannelOutcome<T> {
    val mediaType = spec.settings.mediaType
    val codec = settings.codecs.codecFor(mediaType)
        ?: return ChannelOutcome.Failed(CallFailure.ParameterRefused(mediaType.toString()))
    return when (val opened = ChannelOpening(this).open(call, spec.settings)) {
        is Outcome.Failure -> {
            ChannelOutcome.Failed(opened.failure)
        }

        is Outcome.Success -> {
            val channel = opened.value
            ran(session = channel.session, protocol = channel.protocol, spec = spec, codec = codec, block = block)
        }
    }
}

internal suspend fun <S, R : Any, T> ran(
    session: WebSocketSession,
    protocol: String?,
    spec: ChannelSpec<S, R>,
    codec: Codec,
    block: suspend ChannelSession<S, R>.() -> T,
): ChannelOutcome<T> = coroutineScope {
    val engine = ChannelEngine(session = session, spec = spec, codec = codec)
    val reader = launch { engine.read() }
    try {
        val value = ChannelSession(engine, protocol).block()
        if (!engine.isClosed) engine.close(code = CloseKind.Normal.code, reason = "")
        withTimeoutOrNull(spec.settings.closeTimeout) { reader.join() }
        ChannelOutcome.Completed(value, engine.closed())
    } finally {
        reader.cancel()
        contained { session.cancel() }
    }
}
