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
import com.davils.kore.core.failure.attempted

private const val DPOP: String = "DPoP"

private const val DPOP_NONCE: String = "dpop-nonce"

private const val USE_DPOP_NONCE: String = "use_dpop_nonce"

private const val POST: String = "POST"

private const val BAD_REQUEST: Int = 400

private const val ASSERTION_FAILED: String = "client assertion failed"

internal class EndpointPost(private val transport: OAuthHttp, private val settings: OAuthSettings) {
    private val proof: ClientProof = ClientProof(settings)

    val audienceOf: (String) -> String = { url -> settings.issuer.ifEmpty { url } }

    suspend fun post(url: String, fields: List<Pair<String, String>>, isProven: Boolean): Reply {
        val first = posted(url = url, fields = fields, isProven = isProven)
        val answered = first as? Reply.Answered
        val refused = answered?.takeIf { found -> found.status == BAD_REQUEST }
        val error = refused?.let { found -> OAuthJson.errorOf(found.content) }
        val hasNonce = answered?.run { DPOP_NONCE in headers } == true
        val isNonce = isProven && settings.dpop != null && error == USE_DPOP_NONCE && hasNonce
        return first.takeUnless { isNonce } ?: posted(url = url, fields = fields, isProven = true)
    }

    private suspend fun posted(url: String, fields: List<Pair<String, String>>, isProven: Boolean): Reply {
        val client = proof.of(audienceOf(url)) ?: return Reply.Failed(ASSERTION_FAILED)
        val proofs = settings.dpop?.takeIf { isProven }
        val dpop = proofs?.let { found -> attempted { found.proof(method = POST, url = url) }.getOrNull() }
        val sent = client.headers + listOfNotNull(dpop?.let { found -> DPOP to found })
        val reply = transport.post(url = url, fields = fields + client.fields, headers = sent)
        val nonce = (reply as? Reply.Answered)?.run { headers[DPOP_NONCE] }
        proofs?.acceptNonce(url = url, nonce = nonce)
        return reply
    }
}
