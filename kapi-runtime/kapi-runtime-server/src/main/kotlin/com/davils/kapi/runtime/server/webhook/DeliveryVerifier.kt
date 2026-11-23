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

package com.davils.kapi.runtime.server.webhook

import com.davils.kapi.runtime.server.request.Request
import kotlinx.io.bytestring.ByteString

/**
 * Verifies that a webhook or a callback a server receives comes from the sender it claims, before it is authorized and
 * handled; the service provider interface of the verification of deliveries.
 *
 * The server reads the body of a delivery as a whole within its limits first and hands its exact bytes over, so a
 * verifier can check a signature or a digest over them. A server without a verifier refuses every delivery whose
 * operation declares no security. A verifier is code of the application: it runs contained, and a failure refuses the
 * delivery. It must be safe to call from any coroutine at once. Ready verifiers are [MessageSignatureVerifier] of
 * RFC 9421 and [ContentDigestVerifier] of RFC 9530, combined with [allOf].
 *
 * @since 1.0.0
 */
public fun interface DeliveryVerifier {
    /**
     * Verifies a delivery.
     *
     * @param request the request of the delivery.
     * @param content the exact bytes of its body, after any content coding was removed.
     * @return [Verdict.Accepted] or [Verdict.Refused].
     * @since 1.0.0
     */
    public suspend fun verify(request: Request, content: ByteString): Verdict

    /**
     * Combines verifiers.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns a verifier that accepts a delivery only when every one of [verifiers] does, asked in order; the first
         * refusal decides.
         *
         * @param verifiers the verifiers.
         * @return the combined verifier.
         * @throws IllegalArgumentException if [verifiers] is empty.
         * @since 1.0.0
         */
        public fun allOf(vararg verifiers: DeliveryVerifier): DeliveryVerifier {
            require(verifiers.isNotEmpty()) { "A combined verifier combines at least one verifier" }
            val all = verifiers.toList()
            return DeliveryVerifier { request, content ->
                var verdict: Verdict = Verdict.Accepted
                all.forEach { verifier ->
                    if (verdict == Verdict.Accepted) verdict = verifier.verify(request, content)
                }
                verdict
            }
        }
    }
}
