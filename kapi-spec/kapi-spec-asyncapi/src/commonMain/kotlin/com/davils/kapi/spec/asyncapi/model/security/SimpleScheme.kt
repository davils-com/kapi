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
 * A security scheme without fields of its own: `userPassword`, `X509`, `symmetricEncryption`, `asymmetricEncryption`,
 * `plain`, `scramSha256`, `scramSha512` or `gssapi`.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class SimpleScheme internal constructor(builder: SimpleSchemeBuilder) : SecurityScheme(builder) {
    /**
     * The type of the scheme.
     *
     * @since 1.0.0
     */
    public override val type: SecuritySchemeType = builder.type

    /**
     * A description of the scheme, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public override val description: String? = builder.description

    override val identity: List<Any?> get() = listOf(type, description)
}
