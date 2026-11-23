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

package com.davils.kapi.runtime.testing.mock

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A request and the response it received, in the order a [MockApi] answered them.
 *
 * @property request the request.
 * @property response the response.
 * @property route the path template of the route that answered, such as `/pets/{id}`, or `null` if no route did.
 * @since 1.0.0
 */
public class RecordedExchange(
    public val request: RecordedRequest,
    public val response: RecordedResponse,
    public val route: String? = null,
) {
    private val identity: List<Any?> get() = listOf(request, response, route)

    override fun equals(other: Any?): Boolean = other is RecordedExchange && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String {
        val shown = route?.let(SafeText::quoted) ?: "none"
        return "RecordedExchange($request, $response, route=$shown)"
    }
}
