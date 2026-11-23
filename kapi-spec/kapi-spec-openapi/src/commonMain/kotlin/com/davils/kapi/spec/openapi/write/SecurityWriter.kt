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

package com.davils.kapi.spec.openapi.write

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kaml.core.node.yamlScalar
import com.davils.kapi.spec.openapi.model.security.ApiKeyScheme
import com.davils.kapi.spec.openapi.model.security.HttpScheme
import com.davils.kapi.spec.openapi.model.security.MutualTlsScheme
import com.davils.kapi.spec.openapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.openapi.model.security.OAuthFlow
import com.davils.kapi.spec.openapi.model.security.OAuthFlows
import com.davils.kapi.spec.openapi.model.security.OpenIdConnectScheme
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.security.SecurityScheme
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class SecurityWriter(family: VersionFamily) {
    private val isCurrent: Boolean = family.isAtLeast(VersionFamily.V32)

    fun requirement(requirement: SecurityRequirement): YamlNode = Nodes.mapping(requirement.schemes, Nodes::texts)

    fun scheme(scheme: SecurityScheme): YamlNode {
        val out = Out()
        when (scheme) {
            is ApiKeyScheme -> apiKey(out, scheme)
            is HttpScheme -> http(out, scheme)
            is MutualTlsScheme -> out.text(Keys.TYPE, Keys.MUTUAL_TLS)
            is OAuth2Scheme -> oauth2(out, scheme)
            is OpenIdConnectScheme -> openIdConnect(out, scheme)
        }
        out.text(Keys.DESCRIPTION, scheme.description)
        out.flag(Keys.DEPRECATED, scheme.isDeprecated && isCurrent)
        return out.build(scheme)
    }

    private fun apiKey(out: Out, scheme: ApiKeyScheme) {
        out.text(Keys.TYPE, Keys.API_KEY)
        out.text(Keys.NAME, scheme.name)
        out.text(Keys.IN, scheme.placement.keyword)
    }

    private fun http(out: Out, scheme: HttpScheme) {
        out.text(Keys.TYPE, Keys.HTTP)
        out.text(Keys.SCHEME, scheme.scheme)
        out.text(Keys.BEARER_FORMAT, scheme.bearerFormat)
    }

    private fun oauth2(out: Out, scheme: OAuth2Scheme) {
        out.text(Keys.TYPE, Keys.OAUTH2)
        out.node(Keys.FLOWS, flows(scheme.flows))
        out.text(Keys.OAUTH2_METADATA_URL, scheme.oauth2MetadataUrl?.takeIf { isCurrent })
    }

    private fun openIdConnect(out: Out, scheme: OpenIdConnectScheme) {
        out.text(Keys.TYPE, Keys.OPEN_ID_CONNECT)
        out.text(Keys.OPEN_ID_CONNECT_URL, scheme.openIdConnectUrl)
    }

    private fun flows(flows: OAuthFlows): YamlNode {
        val out = Out()
        out.node(Keys.IMPLICIT, flows.implicit?.let(::flow))
        out.node(Keys.PASSWORD, flows.password?.let(::flow))
        out.node(Keys.CLIENT_CREDENTIALS, flows.clientCredentials?.let(::flow))
        out.node(Keys.AUTHORIZATION_CODE, flows.authorizationCode?.let(::flow))
        out.node(Keys.DEVICE_AUTHORIZATION, flows.deviceAuthorization?.takeIf { isCurrent }?.let(::flow))
        return out.build(flows)
    }

    private fun flow(flow: OAuthFlow): YamlNode {
        val out = Out()
        out.text(Keys.AUTHORIZATION_URL, flow.authorizationUrl)
        out.text(Keys.DEVICE_AUTHORIZATION_URL, flow.deviceAuthorizationUrl?.takeIf { isCurrent })
        out.text(Keys.TOKEN_URL, flow.tokenUrl)
        out.text(Keys.REFRESH_URL, flow.refreshUrl)
        out.node(Keys.SCOPES, Nodes.mapping(flow.scopes) { description -> yamlScalar(description) })
        return out.build(flow)
    }
}
