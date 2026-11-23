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

package com.davils.kapi.runtime.server.operation

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * The use of a security scheme by an operation: the scheme, where its credential lies and the scopes the operation
 * demands, one entry of a security requirement of OpenAPI 3.2 §4.26.
 *
 * @property scheme the name of the scheme, which selects its [com.davils.kapi.runtime.server.security.Authorizer].
 * @property source where the credential lies.
 * @param scopes the scopes the operation demands, such as `pets:write`; none for a scheme without scopes.
 * @since 1.0.0
 */
public class SchemeUse(
    public val scheme: String,
    public val source: CredentialSource,
    scopes: Set<String> = emptySet(),
) {
    /**
     * The scopes the operation demands.
     *
     * @since 1.0.0
     */
    public val scopes: Set<String> = scopes.toSet()

    private val parts: List<Any> get() = listOf(scheme, source, scopes)

    override fun equals(other: Any?): Boolean = other is SchemeUse && parts == other.parts

    override fun hashCode(): Int = parts.hashCode()

    @OptIn(DavilsInternal::class)
    override fun toString(): String = "SchemeUse(${SafeText.text(scheme)}, $source, ${scopes.map(SafeText::text)})"
}
