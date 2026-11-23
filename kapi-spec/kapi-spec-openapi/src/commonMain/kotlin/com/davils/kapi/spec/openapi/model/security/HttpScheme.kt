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

package com.davils.kapi.spec.openapi.model.security

/**
 * A security scheme of `type` `http`: an HTTP authentication scheme of RFC 9110 sent in the `Authorization` header,
 * such as `basic` or `bearer`.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class HttpScheme internal constructor(builder: HttpSchemeBuilder) :
    SecurityScheme(builder) {
    public override val description: String? = builder.description

    /**
     * Whether the scheme is `deprecated` and should no longer be used; `false` when the document leaves it out and
     * before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public override val isDeprecated: Boolean = builder.isDeprecated

    /**
     * The `scheme` of HTTP authentication as it was written, such as `basic` or `bearer`, which names a scheme of the
     * IANA registry and is compared without regard to case; required by OpenAPI, so a reading reports a scheme that
     * leaves it out and keeps it empty here.
     *
     * @since 1.0.0
     */
    public val scheme: String = builder.scheme

    /**
     * The `bearerFormat`: a hint how a bearer token is formatted, such as `JWT`, which serves documentation only;
     * `null` when the document leaves it out.

     *
     * @since 1.0.0
     */
    public val bearerFormat: String? = builder.bearerFormat

    override val identity: List<Any?> get() = listOf(description, scheme, bearerFormat, isDeprecated)
}
