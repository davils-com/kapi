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

internal class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long?,
    val refreshToken: String?,
) {
    companion object {
        fun of(text: String): TokenResponse? {
            val json = OAuthJson.objectOf(text)
            val token = json?.let { found -> OAuthJson.text(found, "access_token") }
            val type = json?.let { found -> OAuthJson.text(found, "token_type") }
            if (json == null || token == null || type == null) return null
            return TokenResponse(
                accessToken = token,
                tokenType = type,
                expiresIn = OAuthJson.number(json, "expires_in"),
                refreshToken = OAuthJson.text(json, "refresh_token"),
            )
        }
    }
}
