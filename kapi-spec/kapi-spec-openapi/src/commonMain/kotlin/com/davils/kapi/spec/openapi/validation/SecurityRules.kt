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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.openapi.model.security.OpenIdConnectScheme
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.security.SecurityScheme
import com.davils.kapi.spec.openapi.resolve
import com.davils.kore.core.annotation.DavilsInternal

internal object SecurityRules {
    fun check(scope: RuleScope) {
        val components = scope.document.components
        val declared = components?.securitySchemes.orEmpty()
        scope.all<SecurityRequirement>().forEach { requirement -> requirement(scope, requirement, declared) }
    }

    private fun requirement(
        scope: RuleScope,
        requirement: SecurityRequirement,
        schemes: Map<String, Referable<SecurityScheme>>,
    ) {
        val unknown = requirement.schemes.keys.filterNot { name -> name in schemes }
        unknown.forEach { name ->
            val message = "The security requirement names $name, which is no security scheme of the components"
            scope.report(DiagnosticCodes.SECURITY_UNKNOWN_SCHEME, message, requirement)
        }
        val scoped = requirement.schemes.entries.filter { (_, scopes) -> scopes.isNotEmpty() }
        val misplaced = scoped.map { (name, _) -> name }.filter { name -> isMisplaced(scope, schemes[name]) }
        val refused = misplaced.takeIf { scope.family == VersionFamily.V30 }
        refused.orEmpty().forEach { name ->
            val message = "The security scheme $name has no scopes, so a requirement must not list any"
            scope.report(DiagnosticCodes.SECURITY_SCOPES, message, requirement)
        }
    }

    private fun isMisplaced(scope: RuleScope, declared: Referable<SecurityScheme>?): Boolean {
        val scheme = declared?.let { found -> scope.document.resolve(found) }
        return declared != null && !hasScopes(scheme)
    }

    private fun hasScopes(scheme: SecurityScheme?): Boolean = scheme is OAuth2Scheme || scheme is OpenIdConnectScheme
}
