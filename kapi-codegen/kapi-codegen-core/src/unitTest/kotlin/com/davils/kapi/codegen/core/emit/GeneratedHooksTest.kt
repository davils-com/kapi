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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.fixture.ServedScenario
import com.davils.kapi.codegen.core.fixture.ServedScenarios
import com.davils.kapi.runtime.client.ClientSettingsBuilder
import com.davils.kapi.runtime.client.pipeline.CallPhase
import com.davils.kapi.runtime.core.security.Credentials
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.core.security.credentials
import com.davils.kapi.runtime.server.ServerSettingsBuilder
import com.davils.kapi.runtime.server.security.ApiKeyAuthorizer
import com.davils.kapi.runtime.server.webhook.DeliveryVerifier
import com.davils.kapi.runtime.server.webhook.Verdict
import com.davils.kore.core.principal.principal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString

private const val DOCUMENT: String = "hooks"

private const val SIGNATURE: String = "X-Signature"

private const val SECRET: String = "shared"

private const val KEY: String = "hook-key"

private const val UNVERIFIED: String = "unexpected 401"

private const val LOST: String = "lost"

private val refused: List<String> = listOf(
    "failed DestinationRefused(HostNotAllowed)",
    "failed DestinationRefused(Url)",
)

private val delivered: List<String> = listOf("noted 1", "noted 2", "unexpected 400", "gone", LOST, "changed") + refused

private val verifying: DeliveryVerifier = DeliveryVerifier { request, content ->
    val isSigned = request.headers[SIGNATURE] == signatureOf(content)
    Verdict.Accepted.takeIf { isSigned } ?: Verdict.Refused("unsigned")
}

private val key: Credentials = credentials { scheme("key") { Secret(KEY) } }

private fun signatureOf(content: ByteString): String {
    val signed = SECRET + content.decodeToString()
    return "v1=" + signed.hashCode()
}

private fun ClientSettingsBuilder.signing() {
    intercept(CallPhase.Authorize, "sign") { trip ->
        val content = trip.call.body?.content ?: ByteString()
        trip.withCall(trip.call.change { header(name = SIGNATURE, value = signatureOf(content)) })
    }
}

private fun ServerSettingsBuilder.receiving(isVerified: Boolean) {
    authorizer("key", ApiKeyAuthorizer { secret -> principal("provider").takeIf { secret.reveal() == KEY } })
    if (isVerified) verifier = verifying
}

private suspend fun played(credentials: Credentials, isSigned: Boolean = true, isVerified: Boolean = true): Any? =
    ServedScenarios.run(
        ServedScenario(
            document = DOCUMENT,
            server = { receiving(isVerified) },
            client = {
                this.credentials = credentials
                if (isSigned) signing()
            },
        ),
    )

class GeneratedHooksTest : FunSpec({
    context("a generated sender delivering to a generated receiver") {
        test("gets every answer of the receiver when it signs what it sends, and sends nowhere its policy refuses") {
            played(key) shouldBe delivered
        }

        test("is refused by a receiver that verifies deliveries when it does not sign them") {
            played(key, isSigned = false) shouldBe List(delivered.size - refused.size) { UNVERIFIED } + refused
        }

        test("reaches only what declares security of its own at a receiver without a verifier") {
            val expected = List(4) { UNVERIFIED } + listOf(LOST, UNVERIFIED) + refused

            played(key, isSigned = false, isVerified = false) shouldBe expected
        }

        test("does not send a delivery whose security it has no credential for") {
            val expected = delivered.map { answer -> "failed CredentialMissing".takeIf { answer == LOST } ?: answer }

            played(Credentials.None) shouldBe expected
        }
    }
})
