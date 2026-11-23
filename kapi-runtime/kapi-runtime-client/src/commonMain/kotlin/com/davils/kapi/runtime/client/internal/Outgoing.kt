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

import com.davils.kapi.runtime.client.call.Body
import com.davils.kapi.runtime.client.call.Upgrade
import com.davils.kapi.runtime.client.call.Upload
import com.davils.kapi.runtime.client.pipeline.RoundTrip
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.parameter.ParameterEntry

private const val IDEMPOTENCY_KEY: String = "Idempotency-Key"

private const val GET: String = "GET"

private const val HEAD: String = "HEAD"

private const val POST: String = "POST"

private const val SEE_OTHER: Int = 303

private val movedStatuses: Set<Int> = setOf(301, 302)

private val sensitiveNames: Set<String> = setOf("authorization", "cookie", "proxy-authorization", "dpop")

private val contentNames: Set<String> = setOf("content-type", "content-length", "content-encoding", "content-language")

internal class Outgoing(
    val method: String,
    val url: String,
    val headers: List<ParameterEntry>,
    val cookies: List<ParameterEntry>,
    val accept: List<MediaRange>,
    val body: Body?,
    val upload: Upload?,
    val upgrade: Upgrade?,
    val binding: DpopBinding?,
) {
    val isReplayable: Boolean get() = upload == null && upgrade == null

    fun redirected(status: Int, target: String, isSameOrigin: Boolean): Outgoing? {
        val isGet = (status == SEE_OTHER && method != HEAD) || (status in movedStatuses && method == POST)
        if (!isGet && !isReplayable) return null
        val kept = headers.filter { entry -> isKept(entry, isGet = isGet, isSameOrigin = isSameOrigin) }
        return Outgoing(
            method = GET.takeIf { isGet } ?: method,
            url = target,
            headers = kept,
            cookies = cookies.takeIf { isSameOrigin }.orEmpty(),
            accept = accept,
            body = body.takeUnless { isGet },
            upload = upload.takeUnless { isGet },
            upgrade = upgrade.takeUnless { isGet },
            binding = binding.takeIf { isSameOrigin },
        )
    }

    private fun isKept(entry: ParameterEntry, isGet: Boolean, isSameOrigin: Boolean): Boolean {
        val name = entry.name.lowercase()
        val isCredential = name in sensitiveNames
        val isContent = name in contentNames
        return (isSameOrigin || !isCredential) && (!isGet || !isContent)
    }

    companion object {
        fun first(trip: RoundTrip, url: String): Outgoing {
            val call = trip.call
            val key = call.idempotencyKey?.let { found ->
                ParameterEntry(name = IDEMPOTENCY_KEY, value = found.toString())
            }
            return Outgoing(
                method = call.method,
                url = url,
                headers = call.headers + listOfNotNull(key),
                cookies = call.cookies,
                accept = call.accept,
                body = call.body,
                upload = call.upload,
                upgrade = call.upgrade,
                binding = trip.granted.binding,
            )
        }
    }
}
