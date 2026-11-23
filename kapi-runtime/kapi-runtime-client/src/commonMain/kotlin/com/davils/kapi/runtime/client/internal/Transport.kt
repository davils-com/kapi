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

import com.davils.kapi.runtime.client.ClientEvent
import com.davils.kapi.runtime.client.ClientSettings
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.RequestAttributes
import com.davils.kapi.runtime.client.call.Timeout
import com.davils.kapi.runtime.client.pipeline.RoundTrip
import com.davils.kapi.runtime.client.redirect.RedirectPolicy
import com.davils.kapi.runtime.core.destination.Destination
import com.davils.kapi.runtime.core.response.Headers
import com.davils.kapi.runtime.core.response.headersOf
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.failure.contained
import com.davils.kore.core.outcome.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.util.toMap
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteArray

private const val COOKIE_SEPARATOR: String = "; "

private const val ACCEPT_SEPARATOR: String = ", "

private const val DPOP: String = "DPoP"

private const val DPOP_NONCE: String = "DPoP-Nonce"

private const val LOCATION: String = "location"

private const val CHUNK: Int = 8_192

private const val TIMEOUT: String = "TimeoutException"

private const val UNNAMED: String = "Throwable"

private val redirectStatuses: Set<Int> = setOf(301, 302, 303, 307, 308)

internal class Transport(
    private val http: HttpClient,
    private val settings: ClientSettings,
    private val telemetry: ClientTelemetry,
) {
    private val destinations: Destinations = Destinations(settings.destinations)

    suspend fun send(trip: RoundTrip): Exchange {
        val sink = currentCoroutineContext()[StreamSink]
        val limits = (trip.call.timeout ?: Timeout()).under(settings.timeout)
        val limit = limits.request.takeIf { sink == null } ?: return delivered(trip, Delivery(trip, limits, sink))
        return withTimeoutOrNull(limit) { delivered(trip, Delivery(trip, limits, sink)) }
            ?: Exchange.Failed(CallFailure.TimedOut)
    }

    private suspend fun delivered(trip: RoundTrip, delivery: Delivery): Exchange {
        val base = settings.baseUrl.takeIf(String::isNotEmpty)?.let { found -> found + trip.call.path }
        val raw = trip.call.destination ?: base ?: return Exchange.Failed(CallFailure.BaseUrlMissing)
        return when (val first = firstOf(trip, raw)) {
            is Outcome.Failure -> Exchange.Failed(first.failure)
            is Outcome.Success -> followed(first.value, delivery)
        }
    }

    private suspend fun firstOf(trip: RoundTrip, raw: String): Outcome<Outgoing, CallFailure> {
        val refusal = destinations.refusal(raw) ?: return builtOf(trip, raw)
        return Outcome.Failure(refusal)
    }

    private fun builtOf(trip: RoundTrip, raw: String): Outcome<Outgoing, CallFailure> {
        val builder = URLBuilder(raw)
        trip.call.query.forEach { entry -> builder.parameters.append(name = entry.name, value = entry.value) }
        return Outcome.Success(Outgoing.first(trip, builder.buildString()))
    }

    private suspend fun followed(first: Outgoing, delivery: Delivery): Exchange {
        val policy = settings.redirects ?: return once(first, delivery)
        val origin = Destinations.destinationOf(Url(first.url))
        var exchange = once(first, delivery)
        var next = hopOf(current = first, exchange = exchange, origin = origin, policy = policy)
        var hops = 0
        while (next != null && hops < policy.maxHops) {
            hops += 1
            reportHop(delivery = delivery, hop = next, number = hops)
            exchange = once(next.outgoing, delivery)
            next = hopOf(current = next.outgoing, exchange = exchange, origin = origin, policy = policy)
        }
        return exchange.takeIf { next == null } ?: Exchange.Failed(CallFailure.TooManyRedirects(policy.maxHops))
    }

    private fun reportHop(delivery: Delivery, hop: Hop, number: Int) {
        telemetry.emit {
            ClientEvent.Redirected(
                api = settings.name,
                operation = delivery.trip.call.operation,
                status = hop.status,
                hop = number,
                isCrossOrigin = !hop.isSameOrigin,
            )
        }
    }

    private fun hopOf(current: Outgoing, exchange: Exchange, origin: Destination, policy: RedirectPolicy): Hop? {
        val answered = (exchange as? Exchange.Answered)?.takeIf { found -> found.status in redirectStatuses }
        val location = answered?.run { headers[LOCATION] }
        val resolved = location?.let { found -> resolvedOrNull(base = current.url, location = found) }
        val isSameOrigin = resolved?.let { found -> origin.isSameOrigin(found.second) } == true
        val isAllowed = isSameOrigin || policy.isCrossOriginAllowed
        val status = answered?.status ?: 0
        val next = resolved?.takeIf { isAllowed }?.let { found ->
            current.redirected(
                status,
                found.first,
                isSameOrigin,
            )
        }
        return next?.let { found -> Hop(outgoing = found, status = status, isSameOrigin = isSameOrigin) }
    }

    private fun resolvedOrNull(base: String, location: String): Pair<String, Destination>? =
        contained { resolvedOf(base = base, location = location) }.getOrNull()

    private fun resolvedOf(base: String, location: String): Pair<String, Destination> {
        val url = References.resolve(base = base, reference = location)
        return url to Destinations.destinationOf(Url(url))
    }

    private suspend fun once(outgoing: Outgoing, delivery: Delivery): Exchange {
        val refusal = destinations.refusal(outgoing.url) ?: return exchanged(outgoing, delivery)
        return Exchange.Failed(refusal)
    }

    private suspend fun exchanged(outgoing: Outgoing, delivery: Delivery): Exchange {
        val result = attempted {
            val binding = outgoing.binding
            val proof = binding?.let { found ->
                found.proofs.proof(method = outgoing.method, url = outgoing.url, accessToken = found.token)
            }
            val statement = http.prepareRequest {
                build(request = this, outgoing = outgoing, proof = proof, limits = delivery.limits)
            }
            statement.execute { response -> received(response, outgoing, delivery.sink) }
        }
        return result.getOrElse(::failedOf)
    }

    private fun failedOf(failure: Throwable): Exchange {
        val name = failure::class.simpleName ?: UNNAMED
        val isTimeout = failure is HttpRequestTimeoutException || name.endsWith(TIMEOUT)
        val reason = CallFailure.TimedOut.takeIf { isTimeout } ?: CallFailure.Unreachable(name)
        return Exchange.Failed(reason)
    }

    private fun build(request: HttpRequestBuilder, outgoing: Outgoing, proof: String?, limits: Timeout) {
        request.method = HttpMethod.parse(outgoing.method)
        request.url(outgoing.url)
        request.expectSuccess = false
        outgoing.headers.forEach { entry -> request.headers.append(name = entry.name, value = entry.value) }
        if (outgoing.cookies.isNotEmpty()) {
            val value = outgoing.cookies.joinToString(COOKIE_SEPARATOR) { cookie -> cookie.name + "=" + cookie.value }
            request.headers.append(name = HttpHeaders.Cookie, value = value)
        }
        if (outgoing.accept.isNotEmpty()) {
            request.headers.append(name = HttpHeaders.Accept, value = outgoing.accept.joinToString(ACCEPT_SEPARATOR))
        }
        proof?.let { found -> request.headers.append(name = DPOP, value = found) }
        limitsOf(request, limits)
        contentOf(outgoing)?.let { content -> request.setBody(content) }
        outgoing.upgrade?.let { upgrade -> request.attributes.put(RequestAttributes.upgrade, upgrade) }
    }

    private fun contentOf(outgoing: Outgoing): OutgoingContent? {
        val upload = outgoing.upload?.let(::UploadContent)
        val body = outgoing.body?.let { found ->
            ByteArrayContent(found.content.toByteArray(), ContentType.parse(found.contentType.toString()))
        }
        return upload ?: body
    }

    private fun limitsOf(request: HttpRequestBuilder, limits: Timeout) {
        if (limits.connect == null && limits.socket == null) return
        request.timeout {
            connectTimeoutMillis = limits.connect?.inWholeMilliseconds
            socketTimeoutMillis = limits.socket?.inWholeMilliseconds
        }
    }

    private suspend fun received(response: HttpResponse, outgoing: Outgoing, sink: StreamSink?): Exchange {
        outgoing.binding?.run { proofs.acceptNonce(url = outgoing.url, nonce = response.headers[DPOP_NONCE]) }
        val headers = headersOf(response.headers.toMap(), settings.sensitiveHeaders)
        val status = response.status.value
        val streaming = sink?.takeIf { found -> found.isStreamed(status, headers) }
        if (streaming == null) return answered(response, headers)
        return streamed(response, headers, streaming)
    }

    private suspend fun streamed(response: HttpResponse, headers: Headers, sink: StreamSink): Exchange {
        val channel = response.bodyAsChannel()
        val chunk = ByteArray(CHUNK)
        var failure: CallFailure? = null
        var count = channel.readAvailable(chunk)
        while (count >= 0) {
            failure = sink.accept(ByteString(chunk, startIndex = 0, endIndex = count))
            count = channel.readAvailable(chunk).takeIf { failure == null } ?: -1
        }
        val ended = failure ?: sink.finish()
        val answered = Exchange.Answered(status = response.status.value, headers = headers, content = ByteString())
        return ended?.let { found -> Exchange.Failed(found) } ?: answered
    }

    private suspend fun answered(response: HttpResponse, headers: Headers): Exchange {
        val limit = settings.maxResponseBytes
        val channel = response.bodyAsChannel()
        val content = channel.readBuffer(limit + 1).readByteArray()
        if (content.size > limit) return Exchange.Failed(CallFailure.TooLarge(limit))
        return Exchange.Answered(status = response.status.value, headers = headers, content = ByteString(content))
    }

    private data class Delivery(val trip: RoundTrip, val limits: Timeout, val sink: StreamSink?)

    private data class Hop(val outgoing: Outgoing, val status: Int, val isSameOrigin: Boolean)
}
