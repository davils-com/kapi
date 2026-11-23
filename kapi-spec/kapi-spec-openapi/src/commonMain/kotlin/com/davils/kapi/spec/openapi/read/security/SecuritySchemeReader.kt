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

package com.davils.kapi.spec.openapi.read.security

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.security.ApiKeyPlacement
import com.davils.kapi.spec.openapi.model.security.ApiKeySchemeBuilder
import com.davils.kapi.spec.openapi.model.security.HttpSchemeBuilder
import com.davils.kapi.spec.openapi.model.security.MutualTlsSchemeBuilder
import com.davils.kapi.spec.openapi.model.security.OAuth2SchemeBuilder
import com.davils.kapi.spec.openapi.model.security.OpenIdConnectSchemeBuilder
import com.davils.kapi.spec.openapi.model.security.SecurityScheme
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object SecuritySchemeReader {
    private val types: Map<String, (Cursor) -> SecurityScheme> = mapOf(
        Keys.API_KEY to ::apiKey,
        Keys.HTTP to ::http,
        Keys.MUTUAL_TLS to ::mutualTls,
        Keys.OAUTH2 to ::oauth2,
        Keys.OPEN_ID_CONNECT to ::openIdConnect,
    )

    fun scheme(cursor: Cursor): SecurityScheme? {
        val type = cursor.entries().firstOrNull { entry -> entry.key == Keys.TYPE }
        val name = type?.run { value.text }
        val isKnown = name in types && isInFamily(cursor, name)
        if (!isKnown) {
            val message = "A security scheme needs a type, which is apiKey, http, oauth2, openIdConnect or mutualTLS"
            val written = type?.value
            val place = written ?: cursor
            place.report(DiagnosticCodes.SECURITY_TYPE, message)
        }
        return types[name].takeIf { isKnown }?.invoke(cursor)
    }

    private fun isInFamily(cursor: Cursor, name: String?): Boolean =
        name != Keys.MUTUAL_TLS || cursor.scope.family.isAtLeast(VersionFamily.V31)

    private fun apiKey(cursor: Cursor): SecurityScheme =
        cursor.read(Shapes.securityScheme, ApiKeySchemeBuilder()) { fields ->
            description = fields.text(Keys.DESCRIPTION)
            name = SecurityReader.required(fields, Keys.NAME)
            placement = placementOf(fields) ?: placement
        }

    private fun http(cursor: Cursor): SecurityScheme =
        cursor.read(Shapes.securityScheme, HttpSchemeBuilder()) { fields ->
            description = fields.text(Keys.DESCRIPTION)
            scheme = SecurityReader.required(fields, Keys.SCHEME)
            bearerFormat = fields.text(Keys.BEARER_FORMAT)
        }

    private fun mutualTls(cursor: Cursor): SecurityScheme =
        cursor.read(Shapes.securityScheme, MutualTlsSchemeBuilder()) { fields ->
            description = fields.text(Keys.DESCRIPTION)
        }

    private fun oauth2(cursor: Cursor): SecurityScheme =
        cursor.read(Shapes.securityScheme, OAuth2SchemeBuilder()) { fields ->
            description = fields.text(Keys.DESCRIPTION)
            val written = fields.cursor(Keys.FLOWS)
            if (written == null) {
                val message = "An oauth2 security scheme needs the field flows"
                fields.cursor.report(DiagnosticCodes.FIELD_MISSING, message)
            }
            written?.let { declared -> flows = SecurityReader.flows(declared) }
        }

    private fun openIdConnect(cursor: Cursor): SecurityScheme =
        cursor.read(Shapes.securityScheme, OpenIdConnectSchemeBuilder()) { fields ->
            description = fields.text(Keys.DESCRIPTION)
            openIdConnectUrl = SecurityReader.required(fields, Keys.OPEN_ID_CONNECT_URL)
        }

    private fun placementOf(fields: Fields): ApiKeyPlacement? {
        val placement = fields.text(Keys.IN)?.let(ApiKeyPlacement::fromKeyword)
        if (placement == null) {
            val message = "An api key needs the field in, which is query, header or cookie"
            fields.report(Keys.IN, DiagnosticCodes.SECURITY_LOCATION, message)
        }
        return placement
    }
}
