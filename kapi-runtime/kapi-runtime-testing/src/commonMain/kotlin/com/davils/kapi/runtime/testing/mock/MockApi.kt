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

@file:OptIn(ExperimentalAtomicApi::class, DavilsInternal::class)

package com.davils.kapi.runtime.testing.mock

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.problem.Problem
import com.davils.kapi.runtime.testing.internal.PathMatch
import com.davils.kapi.runtime.testing.internal.PathTemplates
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.ViolationCode
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val NOT_FOUND: Int = 404

private const val NOT_ALLOWED: Int = 405

private const val FAILED: Int = 500

/**
 * A mock HTTP API on a Ktor [MockEngine]: it answers requests by method and path template through the routes of
 * [mockApi], and records every request and response for assertions, such as the contract checks of
 * [com.davils.kapi.runtime.testing.contract.ContractCheck].
 *
 * A request that no route matches is answered with `404`, or `405` if a route matches its path but not its method,
 * each with a problem of RFC 9457. A route runs contained: what it throws is kept in [failures] and answered with
 * `500`. The engine is safe to call from several coroutines at once; the records keep the order in which the
 * answers finished. A mock API is test support and never belongs on a production class path.
 *
 * @since 1.0.0
 */
public class MockApi internal constructor(builder: MockApiBuilder) : Validatable {
    private val routes: List<Route> = builder.routes

    private val recorded: AtomicReference<List<RecordedExchange>> = AtomicReference(emptyList())

    private val failed: AtomicReference<List<Throwable>> = AtomicReference(emptyList())

    private val lock: Mutex = Mutex()

    /**
     * The path in front of every route.
     *
     * @since 1.0.0
     */
    public val basePath: String = builder.basePath

    /**
     * The codecs that write and read bodies.
     *
     * @since 1.0.0
     */
    public val codecs: Codecs = builder.codecs

    /**
     * The engine that answers; hand it to an [HttpClient] of its own or use [httpClient].
     *
     * @since 1.0.0
     */
    public val engine: MockEngine = MockEngine { data ->
        val exchange = exchangeOf(recordedOf(data))
        lock.withLock { recorded.store(recorded.load() + exchange) }
        val response = exchange.response
        respond(
            content = ByteReadChannel(response.body.toByteArray()),
            status = HttpStatusCode.fromValue(response.status),
            headers = response.headers,
        )
    }

    /**
     * Every exchange so far, in the order the answers finished.
     *
     * @since 1.0.0
     */
    public val exchanges: List<RecordedExchange> get() = recorded.load()

    /**
     * Every request so far, in the order the answers finished.
     *
     * @since 1.0.0
     */
    public val requests: List<RecordedRequest> get() = exchanges.map(RecordedExchange::request)

    /**
     * What the routes threw so far, in order.
     *
     * @since 1.0.0
     */
    public val failures: List<Throwable> get() = failed.load()

    /**
     * Returns an [HttpClient] on [engine] that follows no redirects, as the client runtime of kapi requires.
     *
     * @param configure further configuration of the client.
     * @return the client, which the caller closes.
     * @since 1.0.0
     */
    public fun httpClient(configure: HttpClientConfig<*>.() -> Unit = {}): HttpClient = HttpClient(engine) {
        followRedirects = false
        configure()
    }

    /**
     * Returns the exchanges that the route of [method] and [path] answered.
     *
     * @param method the method, compared without case.
     * @param path the path template of the route, such as `/pets/{id}`.
     * @return the exchanges, in order.
     * @since 1.0.0
     */
    public fun exchangesOf(method: String, path: String): List<RecordedExchange> = exchanges.filter { exchange ->
        exchange.route == path && exchange.request.method.equals(method, ignoreCase = true)
    }

    /**
     * Forgets every exchange and failure recorded so far.
     *
     * @since 1.0.0
     */
    public fun clear() {
        recorded.store(emptyList())
        failed.store(emptyList())
    }

    override fun ValidationScope.validate() {
        val isBasePath = basePath.isEmpty() || (basePath.startsWith('/') && !basePath.endsWith('/'))
        if (!isBasePath) fail(::basePath, "is neither empty nor a path such as /v1 without a slash at its end")
        routes.filterNot { route -> PathTemplates.isTemplate(route.path) }.forEach { route ->
            fail("names ${SafeText.quoted(route.path)}, which is no path template such as /pets/{id}")
        }
        routes.filterNot { route -> isMethod(route.method) }.forEach { route ->
            fail("names ${SafeText.quoted(route.method)}, which is no method such as GET")
        }
        val counts = routes.groupingBy { route -> "${route.method} ${route.path}" }.eachCount()
        counts.filterValues { count -> count > 1 }.keys.forEach { key ->
            fail("answers ${SafeText.quoted(key)} more than once", ViolationCode.DUPLICATE)
        }
    }

    override fun toString(): String = "MockApi(routes=${routes.size}, basePath=${SafeText.quoted(basePath)})"

    private suspend fun recordedOf(data: HttpRequestData): RecordedRequest {
        val content = data.body
        val type = content.contentType?.toString() ?: data.headers[HttpHeaders.ContentType]
        val headers = Headers.build {
            data.headers.forEach { name, values -> appendAll(name, values) }
            remove(HttpHeaders.ContentType)
            type?.let { found -> append(name = HttpHeaders.ContentType, value = found) }
        }
        return RecordedRequest(
            method = data.method.value,
            url = data.url,
            headers = headers,
            body = ByteString(content.toByteArray()),
        )
    }

    private suspend fun exchangeOf(request: RecordedRequest): RecordedExchange {
        val path = relativeOf(request.path)
        val matches = path?.let { found -> matchesOf(found) }.orEmpty()
        val method = request.method.uppercase()
        val candidates = matches.filter { (route, _) -> route.method == method }
        val chosen = candidates.maxByOrNull { (_, match) -> match.literals }
        val response = chosen?.let { (route, match) -> answered(route = route, match = match, request = request) }
        val unmatched = problemOf(status = NOT_ALLOWED, title = "No route of the mock API answers the method")
            .takeIf { matches.isNotEmpty() }
            ?: problemOf(status = NOT_FOUND, title = "No route of the mock API matches the path")
        val route = chosen?.first
        return RecordedExchange(request = request, response = response ?: unmatched, route = route?.path)
    }

    private fun matchesOf(path: String): List<Pair<Route, PathMatch>> = routes.mapNotNull { route ->
        val match = PathTemplates.match(template = route.path, path = path)
        match?.let { found -> route to found }
    }

    private suspend fun answered(route: Route, match: PathMatch, request: RecordedRequest): RecordedResponse {
        val responder = Responder(request = request, codecs = codecs, parameters = match.parameters)
        val outcome = attempted { route.answer(responder) }
        val failure = outcome.exceptionOrNull()
        lock.withLock { failed.store(failed.load() + listOfNotNull(failure)) }
        val answer = responder.answer?.takeIf { outcome.isSuccess }
        val reason = "The route of the mock API failed".takeIf { failure != null }
        val missing = reason ?: "The route of the mock API set no response"
        return answer?.let { found -> responseOf(answer = found, added = responder.headers) }
            ?: problemOf(status = FAILED, title = missing)
    }

    private fun relativeOf(path: String): String? {
        val rest = path.removePrefix(basePath)
        val isBelow = path.startsWith(basePath) && (rest.isEmpty() || rest.startsWith('/'))
        return rest.ifEmpty { "/" }.takeIf { isBelow }
    }

    private fun problemOf(status: Int, title: String): RecordedResponse {
        val problem = Problem(title = title, status = status)
        val text = codecs.json.encodeToString(Problem.serializer(), problem)
        val answer = Answer(
            status = status,
            contentType = MediaType.ProblemJson.toString(),
            content = text.encodeToByteString(),
        )
        return responseOf(answer = answer, added = emptyList())
    }

    private fun responseOf(answer: Answer, added: List<Pair<String, String>>): RecordedResponse {
        val headers = Headers.build {
            answer.contentType?.let { type -> append(name = HttpHeaders.ContentType, value = type) }
            added.forEach { (name, value) -> append(name = name, value = value) }
        }
        return RecordedResponse(status = answer.status, headers = headers, body = answer.content)
    }

    private fun isMethod(method: String): Boolean = method.isNotEmpty() && method.all { char -> char in 'A'..'Z' }
}
