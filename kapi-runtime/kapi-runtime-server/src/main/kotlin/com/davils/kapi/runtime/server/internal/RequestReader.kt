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

import com.davils.kapi.runtime.core.codec.form.FormFailure
import com.davils.kapi.runtime.core.codec.form.FormUrlEncoding
import com.davils.kapi.runtime.core.codec.form.formLimits
import com.davils.kapi.runtime.core.destination.IpAddress
import com.davils.kapi.runtime.core.problem.ProblemViolation
import com.davils.kapi.runtime.core.response.HttpStatus
import com.davils.kapi.runtime.core.response.headersOf
import com.davils.kapi.runtime.server.PeerAttributes
import com.davils.kapi.runtime.server.ServerLimits
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.request.Peer
import com.davils.kapi.runtime.server.request.Request
import com.davils.kore.core.outcome.Outcome
import io.ktor.http.Headers
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.uri

private const val QUERY: String = "query"

private const val HOST: String = "host"

private const val CONTENT_LENGTH: String = "content-length"

private const val TRANSFER_ENCODING: String = "transfer-encoding"

private const val CONTENT_TYPE: String = "content-type"

private const val CONTENT_ENCODING: String = "content-encoding"

private val defaultPorts: Map<String, String> = mapOf("http" to ":80", "https" to ":443")

internal class RequestReader(private val context: ServerContext, private val sensitive: Set<String>) {
    private val limits: ServerLimits = context.limits

    fun read(call: ApplicationCall, operation: String, template: String): RequestRead {
        val uri = call.request.uri
        val rawQuery = uri.substringAfter('?', missingDelimiterValue = "").takeIf { '?' in uri }
        val refusal = headersRefusal(call) ?: lengthRefusal(rawQuery)
        if (refusal != null) return RequestRead.Refused(refusal)
        return when (val decoded = FormUrlEncoding.decode(rawQuery.orEmpty(), queryLimits())) {
            is Outcome.Failure -> RequestRead.Refused(context.problems.invalid(listOf(violationOf(decoded.failure))))

            is Outcome.Success -> RequestRead.Read(
                Request(
                    line = RequestLine(
                        operation = operation,
                        method = call.request.httpMethod.value,
                        scheme = schemeOf(call),
                        authority = authorityOf(call),
                        path = uri.substringBefore('?'),
                        rawQuery = rawQuery,
                    ),
                    pathParameters = parametersOf(call, template),
                    query = decoded.value,
                    headers = headersOf(entriesOf(call), sensitive),
                    peer = peerOf(call),
                    principal = null,
                    body = bodyOf(call),
                    context = context,
                    state = RequestState(),
                ),
            )
        }
    }

    private fun headersRefusal(call: ApplicationCall): Reply? {
        val entries = call.request.headers.entries()
        val count = entries.sumOf { (_, values) -> values.size }
        val bytes = entries.sumOf { (name, values) -> values.sumOf { value -> name.length + value.length } }
        val isTooLarge = count > limits.maxHeaders || bytes > limits.maxHeaderBytes
        val status = HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
        val problem = context.problems.problem(status, ServerCodes.HEADERS_TOO_LARGE)
        return problem.takeIf { isTooLarge }
    }

    private fun lengthRefusal(rawQuery: String?): Reply? {
        val isTooLong = rawQuery.orEmpty().length > limits.maxQueryLength
        return context.problems.problem(HttpStatus.URI_TOO_LONG, ServerCodes.URI_TOO_LONG).takeIf { isTooLong }
    }

    private fun violationOf(failure: FormFailure): ProblemViolation {
        val message = "has more than ${limits.maxQueryParameters} fields".takeIf {
            failure == FormFailure.TooManyFields
        }
        return ProblemViolation(
            path = QUERY,
            code = ServerCodes.MALFORMED,
            message = message ?: "is no percent-encoded UTF-8",
        )
    }

    private fun queryLimits() = formLimits {
        maxLength = limits.maxQueryLength
        maxFields = limits.maxQueryParameters
        maxFieldLength = limits.maxQueryLength
    }

    private fun entriesOf(call: ApplicationCall): Map<String, List<String>> = call.request.headers
        .entries()
        .associate { (name, values) -> name to values }

    private fun parametersOf(call: ApplicationCall, template: String): Map<String, String> {
        val names = PathTemplates.names(template)
        return names.associateWith { name -> call.parameters[name].orEmpty() }
    }

    private fun schemeOf(call: ApplicationCall): String {
        val origin = call.request.origin
        return origin.scheme.lowercase()
    }

    private fun authorityOf(call: ApplicationCall): String {
        val host = call.request.headers[HOST]
            ?.run { trim().lowercase() }
            .orEmpty()
        val scheme = call.request.origin.scheme
            .lowercase()
        return host.removeSuffix(defaultPorts[scheme].orEmpty())
    }

    private fun peerOf(call: ApplicationCall): Peer = Peer(
        address = IpAddress.parseOrNull(call.request.local.remoteAddress),
        certificates = call.attributes.getOrNull(PeerAttributes.certificates).orEmpty(),
        identity = call.attributes.getOrNull(PeerAttributes.identity),
    )

    private fun bodyOf(call: ApplicationCall): BodySource {
        val headers = call.request.headers
        val declared = headers[CONTENT_LENGTH]?.run { trim().toLongOrNull() }
        val encodings = headers.getAll(CONTENT_ENCODING).orEmpty()
        val coding = ContentCoding.of(encodings.joinToString(","))
        return BodySource(
            open = { call.receiveChannel() },
            declaredLength = declared,
            isPresent = isBodyPresent(declared, headers),
            coding = coding,
            limits = limits,
        )
    }

    private fun isBodyPresent(declared: Long?, headers: Headers): Boolean {
        val isUndeclared = declared == null && (headers[TRANSFER_ENCODING] != null || headers[CONTENT_TYPE] != null)
        return isUndeclared || (declared ?: 0L) > 0L
    }
}
