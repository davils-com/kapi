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

package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.operation.plan.SecurityPlan
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.security.ApiKeyScheme
import com.davils.kapi.spec.openapi.model.security.HttpScheme
import com.davils.kapi.spec.openapi.model.security.MutualTlsScheme
import com.davils.kapi.spec.openapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.openapi.model.security.OpenIdConnectScheme
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.security.SecurityScheme
import com.davils.kapi.spec.openapi.resolve

private const val BEARER: String = "Bearer"

private const val BASIC: String = "Basic"

internal class SecurityPlanner(private val scope: PlanScope) {
    private val components = scope.document.components

    private val schemes = components?.securitySchemes.orEmpty()

    fun plan(operation: Operation, isInherited: Boolean): List<List<SecurityPlan>> {
        val inherited = scope.document.security
            .takeIf { isInherited }
            .orEmpty()
        val requirements = operation.security ?: inherited
        return requirements.map { requirement -> alternative(operation, requirement) }
    }

    private fun alternative(operation: Operation, requirement: SecurityRequirement): List<SecurityPlan> =
        requirement.schemes.entries.flatMap { (name, scopes) ->
            val declared = listOfNotNull(schemes[name])
            val resolved = declared.mapNotNull { referable -> scope.document.resolve(referable) }
            val uses = resolved.mapNotNull { scheme -> use(operation, name, scheme) }
            uses.map { use -> use.copy(scopes = scopes) }
        }

    private fun use(operation: Operation, name: String, scheme: SecurityScheme): SecurityPlan? = when (scheme) {
        is HttpScheme -> http(operation, name, scheme)
        is ApiKeyScheme -> SecurityPlan(scheme = name, placement = scheme.placement.name, name = scheme.name)
        is OAuth2Scheme, is OpenIdConnectScheme -> SecurityPlan(scheme = name, placement = BEARER, name = null)
        is MutualTlsScheme -> SecurityPlan(scheme = name, placement = SecurityPlan.CERTIFICATE, name = null)
    }

    private fun http(operation: Operation, name: String, scheme: HttpScheme): SecurityPlan? {
        val placement = listOf(BEARER, BASIC).firstOrNull { known -> known.equals(scheme.scheme, ignoreCase = true) }
        if (placement == null) {
            scope.findings.error(
                CodegenCodes.OPERATION_UNSUPPORTED,
                "The generator has no Kotlin for the HTTP scheme ${shown(scheme.scheme)} of ${shown(name)} yet",
                operation.location,
            )
        }
        return placement?.let { known -> SecurityPlan(scheme = name, placement = known, name = null) }
    }
}
