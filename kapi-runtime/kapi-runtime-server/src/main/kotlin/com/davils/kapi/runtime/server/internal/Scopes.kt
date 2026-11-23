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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.server.security.Authorization
import com.davils.kore.core.failure.contained
import com.davils.kore.core.principal.Principal
import com.davils.kore.core.principal.principal

private const val MOST_SCOPES: Int = 256

internal object Scopes {
    fun split(text: String?): Set<String> {
        val parts = text?.split(' ').orEmpty()
        val scopes = parts.filter(String::isNotEmpty)
        return scopes.take(MOST_SCOPES).toSet()
    }

    fun decide(granted: Set<String>, demanded: Set<String>, principal: () -> Principal?): Authorization {
        if (!granted.containsAll(demanded)) return Authorization.Forbidden(demanded)
        val caller = contained(principal).getOrNull()
        return caller?.let(Authorization::Granted) ?: Authorization.InvalidToken
    }

    fun principalOf(id: String?, scopes: Set<String>): Principal? {
        val subject = id ?: return null
        return principal(subject) {
            scopes.forEach { scope -> roles(scope) }
        }
    }
}
