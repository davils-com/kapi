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

package com.davils.kapi.runtime.client.oauth.internal

import com.davils.kapi.runtime.client.internal.Destinations
import com.davils.kapi.runtime.client.oauth.OAuthSettings
import com.davils.kapi.runtime.core.codec.form.FormUrlEncoding
import com.davils.kapi.runtime.core.response.headersOf
import com.davils.kore.core.failure.attempted
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.util.toMap
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.readByteArray

private const val LIMIT: Long = 64L * 1_024

private const val TIMEOUT: String = "timeout"

private const val TOO_LARGE: String = "too large"

private const val DESTINATION: String = "destination refused"

private const val JSON: String = "application/json"

internal class OAuthHttp(private val http: HttpClient, private val settings: OAuthSettings) {
    private val destinations: Destinations = Destinations(settings.destinations)

    suspend fun post(url: String, fields: List<Pair<String, String>>, headers: List<Pair<String, String>>): Reply =
        exchanged(url) {
            method = HttpMethod.Post
            headers.forEach { (name, value) -> this.headers.append(name = name, value = value) }
            setBody(TextContent(FormUrlEncoding.encode(fields), ContentType.Application.FormUrlEncoded))
        }

    suspend fun get(url: String): Reply = exchanged(url) { method = HttpMethod.Get }

    private suspend fun exchanged(url: String, configure: HttpRequestBuilder.() -> Unit): Reply {
        destinations.refusal(url) ?: return sent(url, configure)
        return Reply.Failed(DESTINATION)
    }

    private suspend fun sent(url: String, configure: HttpRequestBuilder.() -> Unit): Reply {
        val result = withTimeoutOrNull(settings.timeout) {
            attempted {
                val statement = http.prepareRequest {
                    url(url)
                    expectSuccess = false
                    headers.append(name = HttpHeaders.Accept, value = JSON)
                    configure()
                }
                statement.execute { response -> read(response) }
            }
        }
        return result?.getOrElse(::failedOf) ?: Reply.Failed(TIMEOUT)
    }

    private fun failedOf(failure: Throwable): Reply {
        val name = failure::class.simpleName.orEmpty()
        val reason = TIMEOUT.takeIf { name.endsWith("TimeoutException") } ?: name
        return Reply.Failed(reason)
    }

    private suspend fun read(response: HttpResponse): Reply {
        val channel = response.bodyAsChannel()
        val content = channel.readBuffer(LIMIT + 1).readByteArray()
        if (content.size > LIMIT) return Reply.Failed(TOO_LARGE)
        val headers = headersOf(response.headers.toMap())
        return Reply.Answered(status = response.status.value, headers = headers, content = content.decodeToString())
    }
}
