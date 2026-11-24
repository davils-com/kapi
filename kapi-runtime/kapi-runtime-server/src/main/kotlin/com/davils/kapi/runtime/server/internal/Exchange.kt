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
import com.davils.kapi.runtime.server.ApiServer
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.ReplyBody
import com.davils.kapi.runtime.server.request.BodyRefusedException
import com.davils.kapi.runtime.server.request.Request
import com.davils.kollect.core.trace.SpanKind
import com.davils.kollect.core.trace.traced
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.principal.Principal
import com.davils.kore.core.principal.withPrincipal
import io.ktor.server.application.ApplicationCall
import io.ktor.utils.io.ByteWriteChannel

private const val SPAN: String = "kapi.server.request"

private const val VARY: String = "Vary"

private const val ACCEPT: String = "accept"

internal class Exchange(private val server: ApiServer, private val operation: Operation) {
    private val context = server.context

    private val telemetry = server.telemetry

    private val reader = RequestReader(context, server.settings.sensitiveHeaders)

    private val writer = ReplyWriter(context.codecs)

    suspend fun serve(call: ApplicationCall) {
        val started = telemetry.timeSource.markNow()
        val reply = telemetry.tracer.traced(SPAN, SpanKind.Server) { span ->
            span.attribute(key = "kapi.operation", value = operation.name)
            span.attribute(key = "http.request.method", value = operation.method)
            val answer = answer(call)
            span.attribute(key = "http.response.status_code", value = answer.status.toString())
            answer
        }
        telemetry.served(
            operation = operation.name,
            method = operation.method,
            status = reply.status,
            duration = started.elapsedNow(),
        )
        writer.write(call, reply)
    }

    private suspend fun answer(call: ApplicationCall): Reply =
        when (val read = reader.read(call = call, operation = operation.name, template = operation.path)) {
            is RequestRead.Refused -> read.reply
            is RequestRead.Read -> verified(read.request)
        }

    private suspend fun verified(request: Request): Reply {
        if (!operation.isDelivery) return admitted(request)
        return Deliveries.verify(server, operation, request) { delivered -> admitted(delivered) }
    }

    private suspend fun admitted(request: Request): Reply =
        when (val admission = server.gate.admit(operation.security, request)) {
            is Admission.Granted -> varied(request, handled(request.authorized(admission.principal)))
            is Admission.Refused -> context.problems.refused(admission)
        }

    private suspend fun handled(request: Request): Reply {
        val result = attempted { writer.encoded(invoke(request)) }
        val reply = result.getOrElse { failure -> failed(failure) }
        return guarded(reply, request.principal)
    }

    private fun guarded(reply: Reply, principal: Principal?): Reply {
        val stream = reply.body as? ReplyBody.Stream ?: return reply
        val body = ReplyBody.Stream(stream.mediaType, stream.length) { channel: ByteWriteChannel, codecs: Codecs ->
            val written = attempted {
                produced(stream = stream, channel = channel, codecs = codecs, principal = principal)
            }
            written.exceptionOrNull()?.let { failure -> broken(channel, failure) }
        }
        return Reply(status = reply.status, headers = reply.headers, body = body)
    }

    private suspend fun produced(
        stream: ReplyBody.Stream,
        channel: ByteWriteChannel,
        codecs: Codecs,
        principal: Principal?,
    ) {
        if (principal == null) return stream.write(channel, codecs)
        withPrincipal(principal) { stream.write(channel, codecs) }
    }

    private fun broken(channel: ByteWriteChannel, failure: Throwable) {
        telemetry.failed(operation.name, failure)
        channel.cancel(failure)
    }

    private suspend fun invoke(request: Request): Reply {
        val principal = request.principal ?: return operation.handler(request)
        return withPrincipal(principal) { operation.handler(request) }
    }

    private fun failed(failure: Throwable): Reply {
        if (failure is BodyRefusedException) return context.problems.problem(failure.status, failure.code)
        telemetry.failed(operation.name, failure)
        return context.problems.internal()
    }

    private fun varied(request: Request, reply: Reply): Reply {
        if (!request.state.isNegotiated) return reply
        return reply.withDefaultHeader(name = VARY, value = ACCEPT)
    }
}
