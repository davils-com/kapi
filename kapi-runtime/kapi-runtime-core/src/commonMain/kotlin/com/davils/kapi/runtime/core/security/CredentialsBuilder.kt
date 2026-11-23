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

package com.davils.kapi.runtime.core.security

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures [Credentials]: a source for every security scheme that needs a credential and the schemes the
 * connection meets by itself.
 *
 * A builder is not thread-safe; the credentials it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class CredentialsBuilder internal constructor() : ValidatingBuilder<Credentials>() {
    private val added: MutableList<Pair<String, TokenSource>> = mutableListOf()

    private val connected: MutableList<String> = mutableListOf()

    override val subject: String get() = "credentials"

    internal val sources: List<Pair<String, TokenSource>> get() = added

    internal val peers: List<String> get() = connected

    /**
     * Names the source of the credential of the security scheme [name].
     *
     * @param name the name of the scheme in the document, such as `bearerAuth`.
     * @param source the source of its credential.
     * @since 1.0.0
     */
    public fun scheme(name: String, source: TokenSource) {
        checkOpen()
        added += name to source
    }

    /**
     * Names a security scheme the connection meets by itself, such as `mutualTLS`.
     *
     * @param name the name of the scheme in the document.
     * @since 1.0.0
     */
    public fun peer(name: String) {
        checkOpen()
        connected += name
    }

    override fun assemble(): Credentials = Credentials(this)
}

/**
 * Builds the credentials of a client.
 *
 * @param configure names the sources of the schemes.
 * @return the credentials.
 * @throws com.davils.kore.core.validation.report.ValidationException if a scheme is blank or named twice.
 * @since 1.0.0
 */
public fun credentials(configure: CredentialsBuilder.() -> Unit = {}): Credentials {
    val builder = CredentialsBuilder()
    builder.configure()
    return builder.build()
}
