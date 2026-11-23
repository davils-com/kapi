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
 * The type of a security scheme, as AsyncAPI 3.0 defines it.
 *
 * @property keyword the value of the field `type`.
 * @since 1.0.0
 */
public enum class SecuritySchemeType(public val keyword: String) {
    /**
     * A user and a password, read as a [SimpleScheme].
     *
     * @since 1.0.0
     */
    UserPassword("userPassword"),

    /**
     * A key sent as the user or the password, read as an [ApiKeyScheme].
     *
     * @since 1.0.0
     */
    ApiKey("apiKey"),

    /**
     * An X.509 certificate, read as a [SimpleScheme].
     *
     * @since 1.0.0
     */
    X509("X509"),

    /**
     * Symmetric encryption, read as a [SimpleScheme].
     *
     * @since 1.0.0
     */
    SymmetricEncryption("symmetricEncryption"),

    /**
     * Asymmetric encryption, read as a [SimpleScheme].
     *
     * @since 1.0.0
     */
    AsymmetricEncryption("asymmetricEncryption"),

    /**
     * A key sent in a query parameter, a header or a cookie, read as an [HttpApiKeyScheme].
     *
     * @since 1.0.0
     */
    HttpApiKey("httpApiKey"),

    /**
     * An HTTP authentication scheme, read as an [HttpScheme].
     *
     * @since 1.0.0
     */
    Http("http"),

    /**
     * OAuth 2.0, read as an [OAuth2Scheme].
     *
     * @since 1.0.0
     */
    OAuth2("oauth2"),

    /**
     * OpenID Connect, read as an [OpenIdConnectScheme].
     *
     * @since 1.0.0
     */
    OpenIdConnect("openIdConnect"),

    /**
     * SASL PLAIN, read as a [SimpleScheme].
     *
     * @since 1.0.0
     */
    Plain("plain"),

    /**
     * SASL SCRAM-SHA-256, read as a [SimpleScheme].
     *
     * @since 1.0.0
     */
    ScramSha256("scramSha256"),

    /**
     * SASL SCRAM-SHA-512, read as a [SimpleScheme].
     *
     * @since 1.0.0
     */
    ScramSha512("scramSha512"),

    /**
     * SASL GSSAPI, read as a [SimpleScheme].
     *
     * @since 1.0.0
     */
    Gssapi("gssapi"),
}
