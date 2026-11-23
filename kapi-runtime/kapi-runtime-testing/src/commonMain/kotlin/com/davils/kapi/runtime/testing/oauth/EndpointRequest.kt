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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.testing.oauth

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A request that a [FakeAuthorizationServer] received.
 *
 * [toString] shows the endpoint, the grant and the names of the fields, never a value, which may be a secret.
 *
 * @property endpoint the endpoint the request went to.
 * @property url the URL of the request.
 * @property fields the fields of the form the request carried, percent-decoded, in order.
 * @property clientId the client the request authenticated as, from HTTP Basic or the form, or `null` if it named none.
 * @property isAuthenticated whether the client authenticated as the server expects.
 * @since 1.0.0
 */
public class EndpointRequest(
    public val endpoint: Endpoint,
    public val url: String,
    public val fields: List<Pair<String, String>>,
    public val clientId: String?,
    public val isAuthenticated: Boolean,
) {
    private val identity: List<Any?> get() = listOf(endpoint, url, fields, clientId, isAuthenticated)

    /**
     * The `grant_type` of the form, or `null` without one.
     *
     * @since 1.0.0
     */
    public val grantType: String? get() = field("grant_type")

    /**
     * Returns the first value of the field [name].
     *
     * @param name the name of the field.
     * @return the value, or `null` if the form has no such field.
     * @since 1.0.0
     */
    public fun field(name: String): String? = fields.firstOrNull { (found, _) -> found == name }?.second

    override fun equals(other: Any?): Boolean = other is EndpointRequest && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String {
        val names = fields.map { (name, _) -> SafeText.text(name) }
        return "EndpointRequest($endpoint, grant=${grantType?.let(SafeText::quoted) ?: "none"}, fields=$names)"
    }
}
