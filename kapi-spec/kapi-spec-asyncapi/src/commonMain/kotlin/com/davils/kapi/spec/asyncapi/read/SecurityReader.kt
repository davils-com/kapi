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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.diagnostic.shown
import com.davils.kapi.spec.asyncapi.model.security.ApiKeyPlacement
import com.davils.kapi.spec.asyncapi.model.security.ApiKeySchemeBuilder
import com.davils.kapi.spec.asyncapi.model.security.HttpApiKeyPlacement
import com.davils.kapi.spec.asyncapi.model.security.HttpApiKeySchemeBuilder
import com.davils.kapi.spec.asyncapi.model.security.HttpSchemeBuilder
import com.davils.kapi.spec.asyncapi.model.security.OAuth2SchemeBuilder
import com.davils.kapi.spec.asyncapi.model.security.OAuthFlow
import com.davils.kapi.spec.asyncapi.model.security.OAuthFlowBuilder
import com.davils.kapi.spec.asyncapi.model.security.OAuthFlows
import com.davils.kapi.spec.asyncapi.model.security.OAuthFlowsBuilder
import com.davils.kapi.spec.asyncapi.model.security.OpenIdConnectSchemeBuilder
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.asyncapi.model.security.SecuritySchemeType
import com.davils.kapi.spec.asyncapi.model.security.SimpleSchemeBuilder
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.shape.Fields
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.ObjectShape
import com.davils.kapi.spec.asyncapi.read.shape.SecurityShapes
import com.davils.kapi.spec.core.model.Referable

internal object SecurityReader {
    private val typed: Map<SecuritySchemeType, (Cursor) -> SecurityScheme> = mapOf(
        SecuritySchemeType.ApiKey to ::apiKey,
        SecuritySchemeType.HttpApiKey to ::httpApiKey,
        SecuritySchemeType.Http to ::http,
        SecuritySchemeType.OAuth2 to ::oauth2,
        SecuritySchemeType.OpenIdConnect to ::openIdConnect,
    )

    private val types: String = SecuritySchemeType.entries.joinToString { type -> type.keyword }

    fun schemesOf(fields: Fields): List<Referable<SecurityScheme>> =
        fields.items(Keys.SECURITY).referables(ReferenceKind.SecurityScheme, ::scheme)

    fun scheme(cursor: Cursor): SecurityScheme? {
        val type = typeOf(cursor) ?: return null
        return typed[type]?.invoke(cursor) ?: simple(cursor, type)
    }

    private fun typeOf(cursor: Cursor): SecuritySchemeType? {
        if (!cursor.isMapping) {
            cursor.report(DiagnosticCodes.FIELD_TYPE, "A security scheme must be an object")
            return null
        }
        val written = cursor.entries().firstOrNull { entry -> entry.key == Keys.TYPE }?.value
        val keyword = written?.text
        val type = SecuritySchemeType.entries.firstOrNull { candidate -> candidate.keyword == keyword }
        if (type == null) {
            val place = written ?: cursor
            place.report(DiagnosticCodes.SECURITY_TYPE, "A security scheme needs a type, which is one of $types")
        }
        return type
    }

    private fun simple(cursor: Cursor, type: SecuritySchemeType): SecurityScheme =
        cursor.read(SecurityShapes.simple, SimpleSchemeBuilder()) { fields ->
            this.type = type
            description = fields.text(Keys.DESCRIPTION)
        }

    private fun apiKey(cursor: Cursor): SecurityScheme = cursor.read(
        SecurityShapes.apiKey,
        ApiKeySchemeBuilder(),
    ) { fields ->
        description = fields.text(Keys.DESCRIPTION)
        val keywords = ApiKeyPlacement.entries.associateBy(ApiKeyPlacement::keyword)
        placement = placementOf(fields, keywords) ?: placement
    }

    private fun httpApiKey(cursor: Cursor): SecurityScheme =
        cursor.read(SecurityShapes.httpApiKey, HttpApiKeySchemeBuilder()) { fields ->
            description = fields.text(Keys.DESCRIPTION)
            name = fields.text(Keys.NAME).orEmpty()
            val keywords = HttpApiKeyPlacement.entries.associateBy(HttpApiKeyPlacement::keyword)
            placement = placementOf(fields, keywords) ?: placement
        }

    private fun http(cursor: Cursor): SecurityScheme = cursor.read(SecurityShapes.http, HttpSchemeBuilder()) { fields ->
        description = fields.text(Keys.DESCRIPTION)
        scheme = fields.text(Keys.SCHEME).orEmpty()
        bearerFormat = fields.text(Keys.BEARER_FORMAT)
    }

    private fun oauth2(cursor: Cursor): SecurityScheme = cursor.read(
        SecurityShapes.oauth2,
        OAuth2SchemeBuilder(),
    ) { fields ->
        description = fields.text(Keys.DESCRIPTION)
        flows = fields.cursor(Keys.FLOWS)?.let(::flows) ?: flows
        scopes.addAll(fields.texts(Keys.SCOPES))
    }

    private fun openIdConnect(cursor: Cursor): SecurityScheme =
        cursor.read(SecurityShapes.openIdConnect, OpenIdConnectSchemeBuilder()) { fields ->
            description = fields.text(Keys.DESCRIPTION)
            openIdConnectUrl = fields.text(Keys.OPEN_ID_CONNECT_URL).orEmpty()
            scopes.addAll(fields.texts(Keys.SCOPES))
        }

    private fun flows(cursor: Cursor): OAuthFlows = cursor.read(SecurityShapes.flows, OAuthFlowsBuilder()) { fields ->
        implicit = flowOf(fields, Keys.IMPLICIT, SecurityShapes.implicit)
        password = flowOf(fields, Keys.PASSWORD, SecurityShapes.password)
        clientCredentials = flowOf(fields, Keys.CLIENT_CREDENTIALS, SecurityShapes.clientCredentials)
        authorizationCode = flowOf(fields, Keys.AUTHORIZATION_CODE, SecurityShapes.authorizationCode)
    }

    private fun flowOf(fields: Fields, name: String, shape: ObjectShape): OAuthFlow? {
        val written = fields.cursor(name) ?: return null
        return written.read(shape, OAuthFlowBuilder()) { flow ->
            authorizationUrl = flow.text(Keys.AUTHORIZATION_URL)
            tokenUrl = flow.text(Keys.TOKEN_URL)
            refreshUrl = flow.text(Keys.REFRESH_URL)
            availableScopes.putAll(flow.textsByName(Keys.AVAILABLE_SCOPES))
        }
    }

    private fun <T> placementOf(fields: Fields, keywords: Map<String, T>): T? {
        val written = fields.text(Keys.IN) ?: return null
        val placement = keywords[written]
        if (placement == null) {
            val allowed = keywords.keys.joinToString()
            val message = "An api key is sent in one of $allowed, not in ${shown(written)}"
            fields.report(Keys.IN, DiagnosticCodes.SECURITY_LOCATION, message)
        }
        return placement
    }
}
