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

import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.dpop.DpopProofs
import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.security.Secret
import kotlin.io.encoding.Base64

private const val AUTHORIZATION: String = "Authorization"

internal class Granted(
    val headers: List<ParameterEntry> = emptyList(),
    val query: List<ParameterEntry> = emptyList(),
    val cookies: List<ParameterEntry> = emptyList(),
    val secrets: Map<String, Secret> = emptyMap(),
    val binding: DpopBinding? = null,
) {
    fun placed(use: SecurityUse, secret: Secret, proofs: DpopProofs?): Granted {
        val remembered = secrets + (use.scheme to secret)
        val text = secret.reveal()
        val keyed = { name: String -> ParameterEntry(name = name, value = text) }
        return when (val placement = use.placement) {
            CredentialPlacement.Bearer -> bearer(remembered = remembered, secret = secret, proofs = proofs)
            CredentialPlacement.Basic -> header(remembered, "Basic " + Base64.Default.encode(text.encodeToByteArray()))
            is CredentialPlacement.Header -> with(remembered, headers = headers + keyed(placement.name))
            is CredentialPlacement.Query -> with(remembered, query = query + keyed(placement.name))
            is CredentialPlacement.Cookie -> with(remembered, cookies = cookies + keyed(placement.name))
        }
    }

    private fun bearer(remembered: Map<String, Secret>, secret: Secret, proofs: DpopProofs?): Granted {
        val scheme = "DPoP ".takeIf { proofs != null } ?: "Bearer "
        val bound = proofs?.let { found -> DpopBinding(found, secret) } ?: binding
        val entry = ParameterEntry(name = AUTHORIZATION, value = scheme + secret.reveal())
        return Granted(
            headers = headers + entry,
            query = query,
            cookies = cookies,
            secrets = remembered,
            binding = bound,
        )
    }

    private fun header(remembered: Map<String, Secret>, value: String): Granted =
        with(remembered, headers = headers + ParameterEntry(name = AUTHORIZATION, value = value))

    private fun with(
        remembered: Map<String, Secret>,
        headers: List<ParameterEntry> = this.headers,
        query: List<ParameterEntry> = this.query,
        cookies: List<ParameterEntry> = this.cookies,
    ): Granted = Granted(headers = headers, query = query, cookies = cookies, secrets = remembered, binding = binding)
}
