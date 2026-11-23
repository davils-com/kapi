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

package com.davils.kapi.spec.openapi.read.security

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.security.OAuthFlow
import com.davils.kapi.spec.openapi.model.security.OAuthFlowBuilder
import com.davils.kapi.spec.openapi.model.security.OAuthFlows
import com.davils.kapi.spec.openapi.model.security.OAuthFlowsBuilder
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.security.SecurityRequirementBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes
import com.davils.kore.core.annotation.DavilsInternal

internal object SecurityReader {
    fun requirement(cursor: Cursor): SecurityRequirement {
        val builder = SecurityRequirementBuilder()
        builder.location = cursor.location
        if (!cursor.isMapping) {
            cursor.report(DiagnosticCodes.FIELD_TYPE, "A security requirement must be an object")
        }
        cursor.entries().forEach { entry -> builder.schemes[entry.key] = scopes(entry.value) }
        return builder.build()
    }

    fun flows(cursor: Cursor): OAuthFlows = cursor.read(Shapes.oauthFlows, OAuthFlowsBuilder()) { fields ->
        implicit = fields.cursor(Keys.IMPLICIT)?.let(::flow)
        password = fields.cursor(Keys.PASSWORD)?.let(::flow)
        clientCredentials = fields.cursor(Keys.CLIENT_CREDENTIALS)?.let(::flow)
        authorizationCode = fields.cursor(Keys.AUTHORIZATION_CODE)?.let(::flow)
        deviceAuthorization = fields.cursor(Keys.DEVICE_AUTHORIZATION)?.let(::deviceFlow)
    }

    fun required(fields: Fields, name: String): String {
        val text = fields.text(name)
        if (text == null) {
            val message = "A security scheme of this type needs the field $name"
            fields.cursor.report(DiagnosticCodes.FIELD_MISSING, message)
        }
        return text.orEmpty()
    }

    private fun flow(cursor: Cursor): OAuthFlow = cursor.read(Shapes.oauthFlow, OAuthFlowBuilder()) { fields ->
        authorizationUrl = fields.text(Keys.AUTHORIZATION_URL)
        tokenUrl = fields.text(Keys.TOKEN_URL)
        refreshUrl = fields.text(Keys.REFRESH_URL)
        scopes.putAll(fields.textsByName(Keys.SCOPES))
    }

    private fun deviceFlow(cursor: Cursor): OAuthFlow = cursor.read(Shapes.deviceFlow, OAuthFlowBuilder()) { fields ->
        deviceAuthorizationUrl = fields.text(Keys.DEVICE_AUTHORIZATION_URL)
        tokenUrl = fields.text(Keys.TOKEN_URL)
        refreshUrl = fields.text(Keys.REFRESH_URL)
        scopes.putAll(fields.textsByName(Keys.SCOPES))
    }

    private fun scopes(cursor: Cursor): List<String> {
        val items = cursor.items()
        if (cursor.node !is YamlNode.Sequence) {
            cursor.report(DiagnosticCodes.FIELD_TYPE, "The scopes of a security requirement must be an array")
        }
        return items.mapNotNull(Cursor::text)
    }
}
