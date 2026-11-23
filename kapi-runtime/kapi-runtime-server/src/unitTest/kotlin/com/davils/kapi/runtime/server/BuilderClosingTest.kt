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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.server.channel.ChannelLimitsBuilder
import com.davils.kapi.runtime.server.problem.ProblemSettingsBuilder
import com.davils.kapi.runtime.server.security.ApiKeyAuthorizer
import com.davils.kapi.runtime.server.telemetry.TelemetrySettingsBuilder
import com.davils.kapi.runtime.server.webhook.SignaturePolicyBuilder
import com.davils.kollect.core.meterRegistry
import com.davils.konfetti.core.eventBus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class BuilderClosingTest : FunSpec({
    context("a builder that was evaluated") {
        test("refuses every change of a setting or a limit instead of losing it") {
            val limits = ServerLimitsBuilder()
            val channel = ChannelLimitsBuilder()
            val problems = ProblemSettingsBuilder()
            val signatures = SignaturePolicyBuilder()
            limits.build()
            channel.build()
            problems.build()
            signatures.validate()

            shouldThrow<IllegalStateException> { limits.maxHeaders = 1 }
            shouldThrow<IllegalStateException> { channel.maxHeldMessages = 1 }
            shouldThrow<IllegalStateException> { problems.typeBase = "https://example.com/problems/" }
            shouldThrow<IllegalStateException> { signatures.label = "sig" }
        }

        test("refuses every nested block and every added part") {
            val settings = ServerSettingsBuilder()
            val telemetry = TelemetrySettingsBuilder()
            settings.build()
            telemetry.build()

            shouldThrow<IllegalStateException> { settings.realm = "late" }
            shouldThrow<IllegalStateException> { settings.limits { maxHeaders = 1 } }
            shouldThrow<IllegalStateException> { settings.problems { } }
            shouldThrow<IllegalStateException> { settings.telemetry { } }
            shouldThrow<IllegalStateException> { settings.authorizer("key", ApiKeyAuthorizer { null }) }
            shouldThrow<IllegalStateException> { telemetry.meters(meterRegistry()) }
            val scope = CoroutineScope(SupervisorJob())
            shouldThrow<IllegalStateException> { telemetry.events(eventBus(scope)) }
            scope.cancel()
        }
    }
})
