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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.openapi.compatibility

import com.davils.kapi.spec.core.collection.associateKeyed
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.security.ApiKeyScheme
import com.davils.kapi.spec.openapi.model.security.HttpScheme
import com.davils.kapi.spec.openapi.model.security.MutualTlsScheme
import com.davils.kapi.spec.openapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.openapi.model.security.OAuthFlow
import com.davils.kapi.spec.openapi.model.security.OpenIdConnectScheme
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.security.SecurityScheme
import com.davils.kapi.spec.openapi.resolve
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

private const val JOINER: String = "+"

internal object SecurityComparison {
    fun requirements(context: Context, name: String, operations: Both<Operation>) {
        val before = operations.before.security ?: context.baseline.security
        val after = operations.after.security ?: context.candidate.security
        val isOpenBefore = isOpen(before)
        val isOpenAfter = isOpen(after)
        if (isOpenBefore && !isOpenAfter) {
            context.report(
                rule = "security-added",
                impacts = clientBreaking,
                message = "The operation $name requires security",
                before = operations.before,
                after = operations.after,
            )
        }
        if (!isOpenBefore && isOpenAfter) {
            context.report(
                rule = "security-removed",
                impacts = providerBreaking,
                message = "The operation $name no longer requires security",
                before = operations.before,
                after = operations.after,
            )
        }
        if (!isOpenBefore && !isOpenAfter) {
            val requirements = Both(before = before, after = after)
            alternatives(context = context, name = name, requirements = requirements, operations = operations)
        }
    }

    fun schemes(context: Context) {
        val before = context.baseline.components
            ?.securitySchemes
            .orEmpty()
        val after = context.candidate.components
            ?.securitySchemes
            .orEmpty()
        before.forEach { (name, referable) ->
            val old = context.baseline.resolve<SecurityScheme>(referable)
            val new = after[name]?.let { found -> context.candidate.resolve<SecurityScheme>(found) }
            if (old == null || new == null || signatureOf(old) == signatureOf(new)) return@forEach
            context.report(
                rule = "security-scheme-changed",
                impacts = bothBreaking,
                message = "The security scheme ${shown(name)} changes how a request carries its credentials",
                before = old,
                after = new,
            )
        }
    }

    private fun alternatives(
        context: Context,
        name: String,
        requirements: Both<List<SecurityRequirement>>,
        operations: Both<Operation>,
    ) {
        val old = requirements.before.associateKeyed { requirement -> keyOf(requirement) to requirement }
        val new = requirements.after.associateKeyed { requirement -> keyOf(requirement) to requirement }
        old.forEach { (key, requirement) ->
            val found = new[key]
            if (found == null) {
                context.report(
                    rule = "security-requirement-removed",
                    impacts = clientBreaking,
                    message = "The operation $name no longer accepts the security ${shown(key)}",
                    before = requirement,
                    after = operations.after,
                )
            }
            val label = "$name with ${shown(key)}"
            found?.let { candidate ->
                scopes(context = context, label = label, requirements = Both(before = requirement, after = candidate))
            }
        }
        new.forEach { (key, requirement) ->
            if (key in old) return@forEach
            context.report(
                rule = "security-requirement-added",
                impacts = providerBreaking,
                message = "The operation $name now also accepts the security ${shown(key)}",
                before = operations.before,
                after = requirement,
            )
        }
    }

    private fun scopes(context: Context, label: String, requirements: Both<SecurityRequirement>) {
        val old = requirements.before.schemes
        val new = requirements.after.schemes
        val before = OrderedSet(old.values.flatten())
        val after = OrderedSet(new.values.flatten())
        val added = after.filterNot { scope -> scope in before }
        val removed = before.filterNot { scope -> scope in after }
        if (added.isNotEmpty()) {
            context.report(
                rule = "security-scope-added",
                impacts = clientBreaking,
                message = "The security of $label also needs the scopes ${shownAll(added)}",
                before = requirements.before,
                after = requirements.after,
            )
        }
        if (removed.isNotEmpty()) {
            context.report(
                rule = "security-scope-removed",
                impacts = providerBreaking,
                message = "The security of $label no longer needs the scopes ${shownAll(removed)}",
                before = requirements.before,
                after = requirements.after,
            )
        }
    }

    private fun shownAll(scopes: List<String>): String = scopes.joinToString(transform = ::shown)

    private fun isOpen(requirements: List<SecurityRequirement>): Boolean =
        requirements.isEmpty() || requirements.any { requirement -> requirement.schemes.isEmpty() }

    private fun keyOf(requirement: SecurityRequirement): String {
        val names = requirement.schemes.keys
        return names.sorted().joinToString(JOINER)
    }

    private fun signatureOf(scheme: SecurityScheme): String = when (scheme) {
        is ApiKeyScheme -> "apiKey ${scheme.placement.keyword} ${scheme.name}"
        is HttpScheme -> "http ${scheme.scheme.lowercase()}"
        is OAuth2Scheme -> "oauth2 " + flowsOf(scheme).joinToString()
        is OpenIdConnectScheme -> "openIdConnect ${scheme.openIdConnectUrl}"
        is MutualTlsScheme -> "mutualTLS"
    }

    private fun flowsOf(scheme: OAuth2Scheme): List<String> {
        val flows = scheme.flows
        val all = listOf(flows.implicit, flows.password, flows.clientCredentials, flows.authorizationCode)
        return (all + flows.deviceAuthorization).map { flow -> flow?.let(::endpointsOf).orEmpty() }
    }

    private fun endpointsOf(flow: OAuthFlow): String =
        listOf(flow.authorizationUrl, flow.deviceAuthorizationUrl, flow.tokenUrl).joinToString()
}
