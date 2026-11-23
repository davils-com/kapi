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

import kotlinx.serialization.json.JsonObject

internal class DeviceResponse(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String?,
    val expiresIn: Long?,
    val interval: Long?,
) {
    companion object {
        fun of(text: String): DeviceResponse? = OAuthJson.objectOf(text)?.let(::of)

        private fun of(json: JsonObject): DeviceResponse? {
            val parts = listOf(
                "device_code",
                "user_code",
                "verification_uri",
            ).map { name -> OAuthJson.text(json, name) }
            val present = parts.filterNotNull().takeIf { found -> found.size == parts.size } ?: return null
            return DeviceResponse(
                deviceCode = present[0],
                userCode = present[1],
                verificationUri = present[2],
                verificationUriComplete = OAuthJson.text(json, "verification_uri_complete"),
                expiresIn = OAuthJson.number(json, "expires_in"),
                interval = OAuthJson.number(json, "interval"),
            )
        }
    }
}
