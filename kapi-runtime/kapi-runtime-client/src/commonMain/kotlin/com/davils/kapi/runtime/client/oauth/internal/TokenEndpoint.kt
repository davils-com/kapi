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

import com.davils.kapi.runtime.client.oauth.OAuthSettings
import com.davils.kapi.runtime.core.security.Secret
import kotlin.time.Duration.Companion.seconds

private const val OK: Int = 200

private const val BEARER: String = "bearer"

private const val DPOP: String = "dpop"

private const val MALFORMED: String = "malformed answer"

private const val MOST_LIFETIME: Long = 10L * 365 * 24 * 60 * 60

internal class TokenEndpoint(private val post: EndpointPost, private val settings: OAuthSettings) {
    suspend fun request(url: String, fields: List<Pair<String, String>>): Answer {
        val reply = post.post(url = url, fields = fields, isProven = true)
        return when (reply) {
            is Reply.Failed -> Answer.Refused(reply.reason)
            is Reply.Answered -> answerOf(reply)
        }
    }

    fun held(response: TokenResponse): HeldToken? {
        val expected = DPOP.takeIf { settings.dpop != null } ?: BEARER
        val isUsable = response.tokenType.equals(expected, ignoreCase = true) && response.accessToken.isNotEmpty()
        val lifetime = OAuthJson.seconds(value = response.expiresIn, most = MOST_LIFETIME)?.seconds
        val expires = lifetime?.let { found -> settings.timeSource.markNow() + (found - settings.leeway) }
        val refresh = response.refreshToken?.takeIf(String::isNotEmpty)?.let(::Secret)
        val token = HeldToken(access = Secret(response.accessToken), expires = expires, refresh = refresh)
        return token.takeIf { isUsable }
    }

    private fun answerOf(reply: Reply.Answered): Answer {
        if (reply.status != OK) return refusedOf(reply)
        val response = TokenResponse.of(reply.content)
        return response?.let(Answer::Given) ?: Answer.Refused(MALFORMED)
    }

    private fun refusedOf(reply: Reply.Answered): Answer =
        Answer.Refused(reason = "status ${reply.status}", error = OAuthJson.errorOf(reply.content))
}
