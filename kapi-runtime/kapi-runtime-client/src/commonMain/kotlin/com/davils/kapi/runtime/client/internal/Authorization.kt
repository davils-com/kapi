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

import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.dpop.DpopProofs
import com.davils.kapi.runtime.core.security.Credential
import com.davils.kapi.runtime.core.security.Credentials
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.outcome.Outcome

private val cookieOctets: CharRange = '!'..'~'

private const val NOT_IN_COOKIES: String = "\",;\\"

internal class Authorization(private val credentials: Credentials, private val bindings: Map<String, DpopProofs>) {
    suspend fun of(call: Call): Outcome<Granted, CallFailure> {
        val chosen = chosen(call) ?: return unmet(call)
        val start: Outcome<Granted, CallFailure> = Outcome.Success(Granted())
        return chosen.fold(start) { outcome, use -> next(outcome, use) }
    }

    suspend fun refresh(secrets: Map<String, Secret>): List<Pair<String, Boolean>> =
        secrets.map { (scheme, secret) -> scheme to credentials.refresh(scheme, secret) }

    private fun unmet(call: Call): Outcome<Granted, CallFailure> {
        if (call.security.isEmpty()) return Outcome.Success(Granted())
        return Outcome.Failure(CallFailure.CredentialMissing)
    }

    private fun chosen(call: Call): List<SecurityUse>? {
        val schemes = credentials.choose(call.security.map(::schemesOf))
        return call.security.firstOrNull { uses -> schemesOf(uses) == schemes }
    }

    private fun schemesOf(uses: List<SecurityUse>): Set<String> = uses.map(SecurityUse::scheme).toSet()

    private suspend fun next(outcome: Outcome<Granted, CallFailure>, use: SecurityUse): Outcome<Granted, CallFailure> {
        val granted = (outcome as? Outcome.Success)?.value ?: return outcome
        return when (val credential = credentials.credential(use.scheme)) {
            Credential.Missing -> Outcome.Failure(CallFailure.CredentialMissing)
            Credential.Malformed -> Outcome.Failure(CallFailure.CredentialMalformed(use.scheme))
            Credential.Peer -> outcome
            is Credential.Present -> placed(granted = granted, use = use, secret = credential.secret)
        }
    }

    private fun placed(granted: Granted, use: SecurityUse, secret: Secret): Outcome<Granted, CallFailure> {
        val isCookie = use.placement is CredentialPlacement.Cookie
        val isCookieValue = secret.reveal().all { character ->
            character in cookieOctets && character !in NOT_IN_COOKIES
        }
        if (isCookie && !isCookieValue) return Outcome.Failure(CallFailure.CredentialMalformed(use.scheme))
        return Outcome.Success(granted.placed(use, secret, bindings[use.scheme]))
    }
}
