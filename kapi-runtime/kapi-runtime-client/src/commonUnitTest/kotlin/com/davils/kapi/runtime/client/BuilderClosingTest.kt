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

package com.davils.kapi.runtime.client

import com.davils.kapi.runtime.client.channel.ChannelSettingsBuilder
import com.davils.kapi.runtime.client.oauth.OAuthSettingsBuilder
import com.davils.kapi.runtime.client.pipeline.CallPhase
import com.davils.kapi.runtime.client.pipeline.Interceptor
import com.davils.kapi.runtime.client.redirect.RedirectPolicyBuilder
import com.davils.kapi.runtime.client.stream.ReconnectionBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

class BuilderClosingTest : FunSpec({
    context("a builder that was evaluated") {
        test("refuses every change of a setting instead of losing it") {
            val client = ClientSettingsBuilder(isFollowingRedirects = false)
            val oauth = OAuthSettingsBuilder(isFollowingRedirects = false)
            val channel = ChannelSettingsBuilder()
            val redirects = RedirectPolicyBuilder()
            val reconnection = ReconnectionBuilder()
            client.validate()
            oauth.validate()
            channel.build()
            redirects.build()
            reconnection.build()

            shouldThrow<IllegalStateException> { client.baseUrl = "https://api.example.com" }
            shouldThrow<IllegalStateException> { oauth.clientId = "client" }
            shouldThrow<IllegalStateException> { channel.maxPendingRequests = 1 }
            shouldThrow<IllegalStateException> { redirects.maxHops = 1 }
            shouldThrow<IllegalStateException> { reconnection.maxReconnects = 1 }
        }

        test("refuses an interceptor added after it") {
            val client = ClientSettingsBuilder(isFollowingRedirects = false)
            client.build()

            shouldThrow<IllegalStateException> {
                client.intercept(CallPhase.Prepare, "late", Interceptor { trip -> trip })
            }
        }
    }
})
