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

package com.davils.kapi.spec.openapi.model

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.openapi.DocumentBuilder
import com.davils.kapi.spec.openapi.model.component.ComponentsBuilder
import com.davils.kapi.spec.openapi.model.content.EncodingBuilder
import com.davils.kapi.spec.openapi.model.content.ExampleBuilder
import com.davils.kapi.spec.openapi.model.content.LinkBuilder
import com.davils.kapi.spec.openapi.model.content.MediaTypeBuilder
import com.davils.kapi.spec.openapi.model.content.RequestBodyBuilder
import com.davils.kapi.spec.openapi.model.content.ResponseBuilder
import com.davils.kapi.spec.openapi.model.content.ResponsesBuilder
import com.davils.kapi.spec.openapi.model.info.ContactBuilder
import com.davils.kapi.spec.openapi.model.info.ExternalDocsBuilder
import com.davils.kapi.spec.openapi.model.info.InfoBuilder
import com.davils.kapi.spec.openapi.model.info.LicenseBuilder
import com.davils.kapi.spec.openapi.model.info.TagBuilder
import com.davils.kapi.spec.openapi.model.parameter.HeaderBuilder
import com.davils.kapi.spec.openapi.model.parameter.ParameterBuilder
import com.davils.kapi.spec.openapi.model.path.CallbackBuilder
import com.davils.kapi.spec.openapi.model.path.OperationBuilder
import com.davils.kapi.spec.openapi.model.path.PathItemBuilder
import com.davils.kapi.spec.openapi.model.path.PathsBuilder
import com.davils.kapi.spec.openapi.model.schema.DiscriminatorBuilder
import com.davils.kapi.spec.openapi.model.schema.FixedSchemaBuilder
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinitionBuilder
import com.davils.kapi.spec.openapi.model.schema.XmlBuilder
import com.davils.kapi.spec.openapi.model.security.ApiKeySchemeBuilder
import com.davils.kapi.spec.openapi.model.security.HttpSchemeBuilder
import com.davils.kapi.spec.openapi.model.security.MutualTlsSchemeBuilder
import com.davils.kapi.spec.openapi.model.security.OAuth2SchemeBuilder
import com.davils.kapi.spec.openapi.model.security.OAuthFlowBuilder
import com.davils.kapi.spec.openapi.model.security.OAuthFlowsBuilder
import com.davils.kapi.spec.openapi.model.security.OpenIdConnectSchemeBuilder
import com.davils.kapi.spec.openapi.model.security.SecurityRequirementBuilder
import com.davils.kapi.spec.openapi.model.server.ServerBuilder
import com.davils.kapi.spec.openapi.model.server.ServerVariableBuilder

internal val elementBuilders: List<() -> ElementBuilder<*>> = listOf(
    ::ContactBuilder,
    ::LicenseBuilder,
    ::InfoBuilder,
    ::ExternalDocsBuilder,
    ::TagBuilder,
    ::ServerVariableBuilder,
    ::ServerBuilder,
    ::DiscriminatorBuilder,
    ::XmlBuilder,
    ::FixedSchemaBuilder,
    ::SchemaDefinitionBuilder,
    ::ExampleBuilder,
    ::HeaderBuilder,
    ::EncodingBuilder,
    ::MediaTypeBuilder,
    ::ParameterBuilder,
    ::RequestBodyBuilder,
    ::LinkBuilder,
    ::ResponseBuilder,
    ::ResponsesBuilder,
    ::SecurityRequirementBuilder,
    ::OAuthFlowBuilder,
    ::OAuthFlowsBuilder,
    ::ApiKeySchemeBuilder,
    ::HttpSchemeBuilder,
    ::MutualTlsSchemeBuilder,
    ::OAuth2SchemeBuilder,
    ::OpenIdConnectSchemeBuilder,
    ::CallbackBuilder,
    ::OperationBuilder,
    ::PathItemBuilder,
    ::PathsBuilder,
    ::ComponentsBuilder,
    ::DocumentBuilder,
)
