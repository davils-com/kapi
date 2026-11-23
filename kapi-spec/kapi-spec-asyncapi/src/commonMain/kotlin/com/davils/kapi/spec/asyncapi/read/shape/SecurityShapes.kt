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

package com.davils.kapi.spec.asyncapi.read.shape

internal object SecurityShapes {
    private val type: FieldRule = FieldRule(Keys.TYPE, FieldKind.Text, isRequired = true)

    private val description: FieldRule = FieldRule(Keys.DESCRIPTION, FieldKind.Text)

    private val scopes: FieldRule = FieldRule(Keys.SCOPES, FieldKind.Sequence)

    private val availableScopes: FieldRule = FieldRule(Keys.AVAILABLE_SCOPES, FieldKind.Mapping, isRequired = true)

    private val authorizationUrl: FieldRule = FieldRule(Keys.AUTHORIZATION_URL, FieldKind.Text, isRequired = true)

    private val tokenUrl: FieldRule = FieldRule(Keys.TOKEN_URL, FieldKind.Text, isRequired = true)

    private val refreshUrl: FieldRule = FieldRule(Keys.REFRESH_URL, FieldKind.Text)

    private val flowRetired: Map<String, String> = mapOf(Keys.SCOPES to "the scopes of a flow are its availableScopes")

    val simple: ObjectShape = ObjectShape(subject = "A security scheme", fields = listOf(type, description))

    val apiKey: ObjectShape = ObjectShape(
        subject = "An apiKey security scheme",
        fields = listOf(type, description, FieldRule(Keys.IN, FieldKind.Text, isRequired = true)),
    )

    val httpApiKey: ObjectShape = ObjectShape(
        subject = "An httpApiKey security scheme",
        fields = listOf(
            type,
            description,
            FieldRule(Keys.NAME, FieldKind.Text, isRequired = true),
            FieldRule(Keys.IN, FieldKind.Text, isRequired = true),
        ),
    )

    val http: ObjectShape = ObjectShape(
        subject = "An http security scheme",
        fields = listOf(
            type,
            description,
            FieldRule(Keys.SCHEME, FieldKind.Text, isRequired = true),
            FieldRule(Keys.BEARER_FORMAT, FieldKind.Text),
        ),
    )

    val oauth2: ObjectShape = ObjectShape(
        subject = "An oauth2 security scheme",
        fields = listOf(type, description, FieldRule(Keys.FLOWS, FieldKind.Mapping, isRequired = true), scopes),
    )

    val openIdConnect: ObjectShape = ObjectShape(
        subject = "An openIdConnect security scheme",
        fields = listOf(
            type,
            description,
            FieldRule(Keys.OPEN_ID_CONNECT_URL, FieldKind.Text, isRequired = true),
            scopes,
        ),
    )

    val flows: ObjectShape = ObjectShape(
        subject = "The flows of a security scheme",
        fields = listOf(
            FieldRule(Keys.IMPLICIT, FieldKind.Mapping),
            FieldRule(Keys.PASSWORD, FieldKind.Mapping),
            FieldRule(Keys.CLIENT_CREDENTIALS, FieldKind.Mapping),
            FieldRule(Keys.AUTHORIZATION_CODE, FieldKind.Mapping),
        ),
    )

    val implicit: ObjectShape = ObjectShape(
        subject = "An implicit flow",
        fields = listOf(authorizationUrl, refreshUrl, availableScopes),
        retired = flowRetired,
    )

    val password: ObjectShape = ObjectShape(
        subject = "A password flow",
        fields = listOf(tokenUrl, refreshUrl, availableScopes),
        retired = flowRetired,
    )

    val clientCredentials: ObjectShape = ObjectShape(
        subject = "A client credentials flow",
        fields = listOf(tokenUrl, refreshUrl, availableScopes),
        retired = flowRetired,
    )

    val authorizationCode: ObjectShape = ObjectShape(
        subject = "An authorization code flow",
        fields = listOf(authorizationUrl, tokenUrl, refreshUrl, availableScopes),
        retired = flowRetired,
    )
}
