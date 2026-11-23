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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.request.Request
import com.davils.kapi.runtime.server.security.Authorization
import com.davils.kapi.runtime.server.security.AuthorizationAttempt
import com.davils.kapi.runtime.server.security.Authorizer
import com.davils.kore.core.failure.attempted

internal class Gate(private val authorizers: Map<String, Authorizer>, realm: String) {
    private val challenges = Challenges(realm)

    suspend fun admit(security: List<List<SchemeUse>>, request: Request): Admission {
        if (security.isEmpty()) return Admission.Granted(principal = null)
        val outcomes = mutableListOf<Pair<SchemeUse, Authorization>>()
        val granted = security.firstNotNullOfOrNull { alternative ->
            val results = attempt(alternative, request)
            results?.let { found -> outcomes += found }
            val grants = results?.map { (_, outcome) -> outcome as? Authorization.Granted }
            grants?.takeIf { found -> null !in found }?.let { found ->
                Admission.Granted(
                    found.firstOrNull()?.principal,
                )
            }
        }
        return granted ?: refused(security, outcomes)
    }

    private fun refused(security: List<List<SchemeUse>>, outcomes: List<Pair<SchemeUse, Authorization>>): Admission {
        val forbidden = outcomes.firstNotNullOfOrNull { (use, outcome) ->
            (outcome as? Authorization.Forbidden)?.let { found -> use to found }
        }
        val denials = outcomes.mapNotNull { (use, outcome) ->
            (outcome as? Authorization.Denied)?.let { use.scheme to it }
        }
        val headers = forbidden?.let { (use, found) -> challenges.forbidden(use, found) }
            ?: challenges.unauthorized(security.flatten(), denials.toMap())
        return Admission.Refused(isForbidden = forbidden != null, challenges = headers)
    }

    private suspend fun attempt(alternative: List<SchemeUse>, request: Request): List<Pair<SchemeUse, Authorization>>? {
        val credentials = alternative.map { use -> CredentialReader.presentedOf(use.source, request) }
        if (null in credentials) return null
        return alternative.zip(credentials.filterNotNull()).map { (use, credential) ->
            val attempt = AuthorizationAttempt(
                scheme = use.scheme,
                credential = credential,
                scopes = use.scopes,
                request = request,
            )
            val authorizer = authorizers[use.scheme] ?: Authorizer.DenyAll
            val outcome = attempted { authorizer.authorize(attempt) }.getOrNull() ?: Authorization.Unknown
            use to outcome
        }
    }
}
