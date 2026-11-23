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

package com.davils.kapi.codegen.core.asyncapi.read

import com.davils.kapi.codegen.core.asyncapi.GenerationCodes
import com.davils.kapi.codegen.core.asyncapi.Report
import com.davils.kapi.codegen.core.asyncapi.plan.SchemeKind
import com.davils.kapi.codegen.core.asyncapi.plan.SchemeUsePlan
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.security.ApiKeyScheme
import com.davils.kapi.spec.asyncapi.model.security.HttpApiKeyPlacement
import com.davils.kapi.spec.asyncapi.model.security.HttpApiKeyScheme
import com.davils.kapi.spec.asyncapi.model.security.HttpScheme
import com.davils.kapi.spec.asyncapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.asyncapi.model.security.OpenIdConnectScheme
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.asyncapi.model.security.SecuritySchemeType
import com.davils.kapi.spec.asyncapi.model.security.SimpleScheme
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.Referable

private val httpKinds: Map<String, SchemeKind> = mapOf("bearer" to SchemeKind.Bearer, "basic" to SchemeKind.Basic)

internal class SecurityPlanner(private val document: Document, private val report: Report) {
    fun plan(
        channel: String,
        location: Location?,
        operations: List<Operation>,
        servers: List<Server>,
    ): List<List<SchemeUsePlan>> {
        val declared = operations.map { operation -> alternatives(operation.security) }.distinct()
        val served = servers.map { server -> alternatives(server.security) }.distinct()
        val isOwn = declared.any { alternatives -> alternatives.isNotEmpty() }
        val chosen = declared.takeIf { isOwn } ?: served
        if (chosen.size > 1) {
            val text = "The operations or servers of the channel ${shown(channel)} demand different security, but " +
                "one WebSocket handshake authorizes them all"
            report.error(GenerationCodes.SECURITY_MIXED, text, location)
        }
        return chosen.firstOrNull().orEmpty()
    }

    private fun alternatives(security: List<Referable<SecurityScheme>>): List<List<SchemeUsePlan>> =
        security.mapNotNull { referable -> use(referable)?.let(::listOf) }

    private fun use(referable: Referable<SecurityScheme>): SchemeUsePlan? {
        val name = (referable as? Referable.Referenced)?.run { reference.target.pointer.last }
        val scheme = document.resolve(referable)
        val place = scheme?.let(::placeOf)
        val shownName = name?.let(::shown) ?: "written inline"
        if (name == null || scheme == null || place == null) {
            val text = "The security scheme $shownName cannot authorize a WebSocket handshake: a scheme must be a " +
                "named component of the type httpApiKey, http with bearer or basic, oauth2, openIdConnect or X509"
            report.error(GenerationCodes.SECURITY_UNSUPPORTED, text, scheme?.location)
        }
        val scopes = scopesOf(scheme)
        if (name == null || place == null) return null
        return SchemeUsePlan(scheme = name, kind = place.first, field = place.second, scopes = scopes)
    }

    private fun scopesOf(scheme: SecurityScheme?): List<String> = when (scheme) {
        is OAuth2Scheme -> scheme.scopes
        is OpenIdConnectScheme -> scheme.scopes
        is ApiKeyScheme, is HttpApiKeyScheme, is HttpScheme, is SimpleScheme, null -> emptyList()
    }

    private fun placeOf(scheme: SecurityScheme): Pair<SchemeKind, String>? = when (scheme) {
        is HttpApiKeyScheme -> kindOf(scheme.placement) to scheme.name
        is HttpScheme -> httpKindOf(scheme.scheme)?.let { kind -> kind to "" }
        is OAuth2Scheme, is OpenIdConnectScheme -> SchemeKind.Bearer to ""
        is SimpleScheme -> (SchemeKind.Certificate to "").takeIf { scheme.type == SecuritySchemeType.X509 }
        is ApiKeyScheme -> null
    }

    private fun kindOf(placement: HttpApiKeyPlacement): SchemeKind = when (placement) {
        HttpApiKeyPlacement.Query -> SchemeKind.Query
        HttpApiKeyPlacement.Header -> SchemeKind.Header
        HttpApiKeyPlacement.Cookie -> SchemeKind.Cookie
    }

    private fun httpKindOf(scheme: String): SchemeKind? = httpKinds[scheme.lowercase()]
}
