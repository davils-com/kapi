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

package com.davils.kapi.spec.asyncapi.model.security

/**
 * A security scheme of type `http`: an HTTP authentication scheme such as `basic` or `bearer`.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class HttpScheme internal constructor(builder: HttpSchemeBuilder) : SecurityScheme(builder) {
    override val type: SecuritySchemeType get() = SecuritySchemeType.Http

    /**
     * A description of the scheme, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public override val description: String? = builder.description

    /**
     * The name of the HTTP authentication scheme, such as `bearer`.
     *
     * @since 1.0.0
     */
    public val scheme: String = builder.scheme

    /**
     * How a bearer token is formatted, such as `JWT`.
     *
     * @since 1.0.0
     */
    public val bearerFormat: String? = builder.bearerFormat

    override val identity: List<Any?> get() = listOf(description, scheme, bearerFormat)
}
