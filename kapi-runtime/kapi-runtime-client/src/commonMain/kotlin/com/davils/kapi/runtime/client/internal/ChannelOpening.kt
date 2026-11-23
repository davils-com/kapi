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

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.channel.ChannelSettings
import com.davils.kapi.runtime.client.channel.CloseKind
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.outcome.Outcome
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.coroutines.withTimeoutOrNull

private const val PROTOCOL: String = "Sec-WebSocket-Protocol"

private const val COOKIE_SEPARATOR: String = "; "

private const val PROTOCOL_SEPARATOR: String = ", "

private const val SWITCHING: Int = 101

private val toHttp: List<Pair<String, String>> = listOf("wss://" to "https://", "ws://" to "http://")

private val toWebSocket: List<Pair<String, String>> = listOf("https://" to "wss://", "http://" to "ws://")

internal class ChannelOpening(private val client: ApiClient) {
    private val settings = client.settings

    private val destinations: Destinations = Destinations(settings.destinations)

    suspend fun open(call: Call, channel: ChannelSettings): Outcome<OpenedChannel, CallFailure> {
        val isInstalled = client.http.pluginOrNull(WebSockets) != null
        val failure = call.refusal ?: CallFailure.ChannelsNotInstalled.takeUnless { isInstalled }
        if (failure != null) return Outcome.Failure(failure)
        return when (val granted = client.authorization.of(call)) {
            is Outcome.Failure -> granted
            is Outcome.Success -> located(call.withCredentials(granted.value), channel)
        }
    }

    private suspend fun located(call: Call, channel: ChannelSettings): Outcome<OpenedChannel, CallFailure> {
        val base = settings.baseUrl.takeIf(String::isNotEmpty)?.let { found -> found + call.path }
        val raw = call.destination ?: base ?: return Outcome.Failure(CallFailure.BaseUrlMissing)
        return targeted(raw = replaced(raw, toHttp), call = call, channel = channel)
    }

    private suspend fun targeted(
        raw: String,
        call: Call,
        channel: ChannelSettings,
    ): Outcome<OpenedChannel, CallFailure> {
        val refusal = destinations.refusal(raw) ?: return handshake(
            url = replaced(built(raw, call), toWebSocket),
            call = call,
            channel = channel,
        )
        return Outcome.Failure(refusal)
    }

    private fun built(raw: String, call: Call): String {
        val builder = URLBuilder(raw)
        call.query.forEach { entry -> builder.parameters.append(name = entry.name, value = entry.value) }
        return builder.buildString()
    }

    private fun replaced(url: String, schemes: List<Pair<String, String>>): String {
        val match = schemes.firstOrNull { (from, _) -> url.startsWith(from, ignoreCase = true) } ?: return url
        return match.second + url.substring(match.first.length)
    }

    private suspend fun handshake(
        url: String,
        call: Call,
        channel: ChannelSettings,
    ): Outcome<OpenedChannel, CallFailure> {
        val limit = settings.timeout.request
        val opened = attempted {
            val session = withTimeoutOrNull(limit ?: channel.closeTimeout * HANDSHAKE_FACTOR) {
                client.http.webSocketSession { prepared(builder = this, url = url, call = call, channel = channel) }
            }
            session ?: throw HandshakeTimeout()
        }
        return opened.fold(
            onSuccess = { session -> chosen(session, channel) },
            onFailure = { failure -> Outcome.Failure(failureOf(failure)) },
        )
    }

    private fun prepared(builder: HttpRequestBuilder, url: String, call: Call, channel: ChannelSettings) {
        builder.url(url)
        call.headers.forEach { entry -> builder.headers.append(name = entry.name, value = entry.value) }
        if (call.cookies.isNotEmpty()) {
            val cookies = call.cookies.joinToString(COOKIE_SEPARATOR) { cookie -> cookie.name + "=" + cookie.value }
            builder.headers.append(name = HttpHeaders.Cookie, value = cookies)
        }
        if (channel.subprotocols.isNotEmpty()) {
            builder.headers.append(name = PROTOCOL, value = channel.subprotocols.joinToString(PROTOCOL_SEPARATOR))
        }
    }

    private suspend fun chosen(
        session: DefaultClientWebSocketSession,
        channel: ChannelSettings,
    ): Outcome<OpenedChannel, CallFailure> {
        session.pingIntervalMillis = channel.pingInterval.inWholeMilliseconds
        session.timeoutMillis = channel.pongTimeout.inWholeMilliseconds
        session.maxFrameSize = channel.maxFrameBytes
        val protocol = session.call.response.headers[PROTOCOL]
        val isOffered = protocol == null || protocol in channel.subprotocols
        if (isOffered) return Outcome.Success(OpenedChannel(session = session, protocol = protocol))
        session.close(CloseReason(code = CloseKind.ProtocolError.code.toShort(), message = ""))
        return Outcome.Failure(CallFailure.HandshakeRefused(SWITCHING))
    }

    private fun failureOf(failure: Throwable): CallFailure {
        if (failure is HandshakeTimeout) return CallFailure.TimedOut
        val message = (failure as? WebSocketException)?.message.orEmpty()
        val status = message.substringAfterLast(' ').toIntOrNull()
        return status?.let(
            CallFailure::HandshakeRefused,
        ) ?: CallFailure.Unreachable(failure::class.simpleName.orEmpty())
    }

    private class HandshakeTimeout : IllegalStateException("The handshake took too long")

    private companion object {
        const val HANDSHAKE_FACTOR: Int = 6
    }
}
