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

package com.davils.kapi.runtime.server.channel

import com.davils.kapi.runtime.core.header.FieldSyntax
import com.davils.kapi.runtime.server.internal.PathTemplates
import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.request.Request
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A WebSocket channel of AsyncAPI 3 with its binding `ws` that a server serves: its path, its subprotocols, its
 * security, the codecs of its messages, its limits and its handler.
 *
 * A request for the channel is checked and authorized like the request of an operation before it is upgraded, so an
 * unauthorized client never gets a WebSocket. When [protocols] is not empty, the client must offer one of them in
 * `Sec-WebSocket-Protocol`; the server picks the first of [protocols] the client offers, and refuses a client that
 * offers none with a problem with status 400. A request that is no upgrade is refused with 426 (Upgrade Required).
 * Once the request is authorized and before it is upgraded, [admission] reads what the handshake carries, such as
 * the parameters of the channel or its query, and refuses an invalid handshake with the reply it returns, usually the
 * problem of `request.input().refused()` with status 400, so the client learns of it as an HTTP answer instead of a
 * close with 1008 (Policy Violation) after the upgrade. The admission and the handler are code of the application:
 * they run contained with the principal of the request, a failure of the admission answers with a problem with status
 * 500 and a failure of the handler closes the channel with 1011, and neither reveals anything of the failure. An
 * endpoint is immutable.
 *
 * @param Receive the type of the messages the server receives.
 * @param Send the type of the messages the server sends.
 * @property name the name of the channel, which names it in telemetry.
 * @property path the path template, such as `/rooms/{roomId}`.
 * @property receive the codec of the messages the server receives.
 * @property send the codec of the messages the server sends.
 * @param protocols the subprotocols the channel speaks, in the order the server prefers them.
 * @param security the alternatives of security requirements, in order.
 * @property limits the limits of the channel.
 * @property correlation how requests the server sends pair with the replies of the client, or `null` for a channel
 * without such requests.
 * @property admission checks the authorized request before it is upgraded and returns the reply that refuses it, or
 * `null` to upgrade it; it admits every request by default.
 * @property handler serves a session.
 * @throws IllegalArgumentException if [name] is blank, [path] is no path template or a protocol is no token.
 * @since 1.0.0
 */
public class ChannelEndpoint<Receive : Any, Send : Any>(
    public val name: String,
    public val path: String,
    public val receive: MessageCodec<Receive>,
    public val send: MessageCodec<Send>,
    protocols: List<String> = emptyList(),
    security: List<List<SchemeUse>> = emptyList(),
    public val limits: ChannelLimits = ChannelLimits.Default,
    public val correlation: Correlation<Send, Receive>? = null,
    public val admission: suspend (Request) -> Reply? = { null },
    public val handler: suspend ChannelSession<Receive, Send>.() -> Unit,
) {
    /**
     * The subprotocols the channel speaks, in the order the server prefers them.
     *
     * @since 1.0.0
     */
    public val protocols: List<String> = protocols.toList()

    /**
     * The alternatives of security requirements, in order.
     *
     * @since 1.0.0
     */
    public val security: List<List<SchemeUse>> = security.map(List<SchemeUse>::toList)

    init {
        require(name.isNotBlank()) { "A channel has a name" }
        require(PathTemplates.isTemplate(path)) { "The path of a channel is a template such as /rooms/{roomId}" }
        require(protocols.all(FieldSyntax::isToken)) { "A subprotocol is a token" }
    }

    override fun toString(): String = "ChannelEndpoint(${SafeText.text(name)}, ${SafeText.text(path)})"
}
