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

package com.davils.kapi.runtime.client.page

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.internal.Destinations
import com.davils.kapi.runtime.client.internal.PageStep
import com.davils.kapi.runtime.client.internal.References
import com.davils.kapi.runtime.core.header.Links
import com.davils.kapi.runtime.core.response.isSuccess
import com.davils.kore.core.failure.contained
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val LINK: String = "link"

private const val DEFAULT_PAGES: Int = 1_000

/**
 * Pages through an operation that links each page to the next with `Link: <...>; rel="next"` of RFC 8288, as GitHub
 * and many other APIs do.
 *
 * The first page is [call]; each next page is the same call sent to the target of the `next` link, resolved against
 * the URL of the page that names it. The flow ends after a page that is no successful answer, names no `next` link,
 * names a malformed one or the URL it was asked with, or after [maxPages] pages. A link to another origin ends the
 * flow with [CallFailure.CrossOrigin], because the credentials of the call must not follow it; every link passes the
 * destination policy of the client when it is sent. The flow is cold and asks for a page only when the collector
 * wants the next one.
 *
 * @receiver the client.
 * @param call the call of the first page.
 * @param maxPages the most pages, at least 1.
 * @return the exchange of every page in its order.
 * @throws IllegalArgumentException if [maxPages] is below 1.
 * @since 1.0.0
 */
public fun ApiClient.linkPages(call: Call, maxPages: Int = DEFAULT_PAGES): Flow<Exchange> {
    require(maxPages >= 1) { "A flow of pages holds at least one page" }
    return flow {
        var current: Call? = call
        var pages = 0
        while (current != null && pages < maxPages) {
            val exchange = execute(current)
            emit(exchange)
            pages += 1
            val next = nextOf(current, exchange)
            next.failure?.let { failure -> emit(failure) }
            current = next.call
        }
    }
}

internal fun ApiClient.nextOf(call: Call, exchange: Exchange): PageStep {
    val answered = (exchange as? Exchange.Answered)?.takeIf { found -> found.isSuccess }
    val field = answered?.run { headers[LINK] }
    val links = field?.let(Links::parseOrNull).orEmpty()
    val target = Links.find(links, Links.NEXT)?.target ?: return PageStep()
    return stepOf(call = call, target = target)
}

internal fun ApiClient.urlOf(call: Call): String? {
    val base = settings.baseUrl.takeIf(String::isNotEmpty)?.let { found -> found + call.path }
    val raw = call.destination ?: base ?: return null
    return contained {
        val builder = URLBuilder(raw)
        call.query.forEach { entry -> builder.parameters.append(name = entry.name, value = entry.value) }
        builder.buildString()
    }.getOrNull()
}

private fun ApiClient.stepOf(call: Call, target: String): PageStep {
    val base = urlOf(call) ?: return PageStep()
    return contained { resolvedOf(base = base, target = target) }.fold(
        onSuccess = { resolved -> followed(call = call, base = base, resolved = resolved) },
        onFailure = { _ -> PageStep() },
    )
}

private fun followed(call: Call, base: String, resolved: Pair<String, Boolean>): PageStep {
    val (url, isSameOrigin) = resolved
    val following = url.takeIf { isSameOrigin && url != base }
    val crossed = Exchange.Failed(CallFailure.CrossOrigin).takeUnless { isSameOrigin }
    return PageStep(call = following?.let(call::withoutQuery), failure = crossed)
}

private fun resolvedOf(base: String, target: String): Pair<String, Boolean> {
    val url = References.resolve(base = base, reference = target)
    val origin = Destinations.destinationOf(Url(base))
    return url to origin.isSameOrigin(Destinations.destinationOf(Url(url)))
}
