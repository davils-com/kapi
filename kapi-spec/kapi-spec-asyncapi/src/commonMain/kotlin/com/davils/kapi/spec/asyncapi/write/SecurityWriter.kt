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

package com.davils.kapi.spec.asyncapi.write

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlScalar
import com.davils.kapi.spec.asyncapi.model.security.ApiKeyScheme
import com.davils.kapi.spec.asyncapi.model.security.HttpApiKeyScheme
import com.davils.kapi.spec.asyncapi.model.security.HttpScheme
import com.davils.kapi.spec.asyncapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.asyncapi.model.security.OAuthFlow
import com.davils.kapi.spec.asyncapi.model.security.OAuthFlows
import com.davils.kapi.spec.asyncapi.model.security.OpenIdConnectScheme
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.asyncapi.model.security.SimpleScheme
import com.davils.kapi.spec.asyncapi.read.shape.Keys

internal class SecurityWriter {
    fun scheme(scheme: SecurityScheme): YamlNode {
        val out = Out()
        out.text(key = Keys.TYPE, value = scheme.type.keyword)
        out.text(key = Keys.DESCRIPTION, value = scheme.description)
        when (scheme) {
            is SimpleScheme -> Unit
            is ApiKeyScheme -> out.text(key = Keys.IN, value = scheme.placement.keyword)
            is HttpApiKeyScheme -> httpApiKey(out, scheme)
            is HttpScheme -> http(out, scheme)
            is OAuth2Scheme -> oauth2(out, scheme)
            is OpenIdConnectScheme -> openIdConnect(out, scheme)
        }
        return out.build(scheme)
    }

    private fun httpApiKey(out: Out, scheme: HttpApiKeyScheme) {
        out.text(key = Keys.NAME, value = scheme.name)
        out.text(key = Keys.IN, value = scheme.placement.keyword)
    }

    private fun http(out: Out, scheme: HttpScheme) {
        out.text(key = Keys.SCHEME, value = scheme.scheme)
        out.text(key = Keys.BEARER_FORMAT, value = scheme.bearerFormat)
    }

    private fun oauth2(out: Out, scheme: OAuth2Scheme) {
        out.node(Keys.FLOWS, flows(scheme.flows))
        out.texts(Keys.SCOPES, scheme.scopes)
    }

    private fun openIdConnect(out: Out, scheme: OpenIdConnectScheme) {
        out.text(key = Keys.OPEN_ID_CONNECT_URL, value = scheme.openIdConnectUrl)
        out.texts(Keys.SCOPES, scheme.scopes)
    }

    private fun flows(flows: OAuthFlows): YamlNode {
        val out = Out()
        out.node(Keys.IMPLICIT, flows.implicit?.let(::flow))
        out.node(Keys.PASSWORD, flows.password?.let(::flow))
        out.node(Keys.CLIENT_CREDENTIALS, flows.clientCredentials?.let(::flow))
        out.node(Keys.AUTHORIZATION_CODE, flows.authorizationCode?.let(::flow))
        return out.build(flows)
    }

    private fun flow(flow: OAuthFlow): YamlNode {
        val out = Out()
        out.text(key = Keys.AUTHORIZATION_URL, value = flow.authorizationUrl)
        out.text(key = Keys.TOKEN_URL, value = flow.tokenUrl)
        out.text(key = Keys.REFRESH_URL, value = flow.refreshUrl)
        out.node(Keys.AVAILABLE_SCOPES, Nodes.mapping(flow.availableScopes) { description -> yamlScalar(description) })
        return out.build(flow)
    }
}
