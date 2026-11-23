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

import com.davils.kapi.runtime.core.response.HttpStatus
import com.davils.kapi.runtime.server.ApiServer
import com.davils.kapi.runtime.server.channel.ChannelEndpoint
import com.davils.kapi.runtime.server.channel.ChannelSession
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.request.Request
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.principal.withPrincipal
import io.ktor.server.application.ApplicationCall
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.util.AttributeKey

private const val UPGRADE: String = "upgrade"

private const val WEBSOCKET: String = "websocket"

internal class ChannelRunner<Receive : Any, Send : Any>(
    private val server: ApiServer,
    private val endpoint: ChannelEndpoint<Receive, Send>,
) {
    private val context = server.context

    private val telemetry = server.telemetry

    private val reader = RequestReader(context, server.settings.sensitiveHeaders)

    private val writer = ReplyWriter(context.codecs)

    suspend fun admit(call: ApplicationCall) {
        val read = reader.read(call = call, operation = endpoint.name, template = endpoint.path)
        val reply = when (read) {
            is RequestRead.Refused -> read.reply
            is RequestRead.Read -> admitted(call, read.request)
        }
        reply?.let { found -> writer.write(call, found) }
    }

    suspend fun refuseProtocol(call: ApplicationCall) {
        writer.write(call, context.problems.problem(HttpStatus.BAD_REQUEST, ServerCodes.UPGRADE_REQUIRED))
    }

    suspend fun run(session: DefaultWebSocketServerSession, protocol: String?) {
        val request = session.call.attributes[REQUEST]
        val limits = endpoint.limits
        session.maxFrameSize = limits.maxFrameBytes.toLong()
        session.pingIntervalMillis = limits.pingInterval.inWholeMilliseconds
        session.timeoutMillis = limits.timeout.inWholeMilliseconds
        val started = telemetry.timeSource.markNow()
        telemetry.opened(endpoint.name, protocol)
        val typed = TypedSession(
            session = session,
            request = request,
            protocol = protocol,
            endpoint = endpoint,
            codecs = context.codecs,
        )
        val failure = attempted { handle(typed, request) }.exceptionOrNull()
        failure?.let { found -> telemetry.failed(endpoint.name, found) }
        typed.close(ChannelSession.INTERNAL_ERROR.takeIf { failure != null } ?: ChannelSession.NORMAL, "")
        telemetry.closed(channel = endpoint.name, code = typed.closeCode, duration = started.elapsedNow())
    }

    private suspend fun admitted(call: ApplicationCall, request: Request): Reply? {
        val isUpgrade = request.headers.all(UPGRADE).any { value -> value.trim().equals(WEBSOCKET, ignoreCase = true) }
        if (!isUpgrade) return upgradeRequired()
        return when (val admission = server.gate.admit(endpoint.security, request)) {
            is Admission.Refused -> {
                context.problems.refused(admission)
            }

            is Admission.Granted -> {
                checked(call, request.authorized(admission.principal))
            }
        }
    }

    private suspend fun checked(call: ApplicationCall, request: Request): Reply? {
        val checked = attempted { withPrincipalOf(request) { endpoint.admission(request) } }
        val refusal = checked.getOrElse { failure -> failed(failure) }
        if (refusal == null) call.attributes.put(REQUEST, request)
        return refusal
    }

    private fun failed(failure: Throwable): Reply {
        telemetry.failed(endpoint.name, failure)
        return context.problems.internal()
    }

    private suspend fun <T> withPrincipalOf(request: Request, block: suspend () -> T): T {
        val principal = request.principal ?: return block()
        return withPrincipal(principal) { block() }
    }

    private suspend fun handle(session: TypedSession<Receive, Send>, request: Request) {
        withPrincipalOf(request) { endpoint.handler(session) }
    }

    private fun upgradeRequired() = context.problems.problem(
        status = HttpStatus.UPGRADE_REQUIRED,
        code = ServerCodes.UPGRADE_REQUIRED,
        headers = listOf("Upgrade" to WEBSOCKET),
    )

    private companion object {
        val REQUEST: AttributeKey<Request> = AttributeKey("kapi.channel.request")
    }
}
