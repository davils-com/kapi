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

import com.davils.kapi.runtime.server.operation.CredentialSource
import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.security.Authorization

internal class Challenges(private val realm: String) {
    fun unauthorized(uses: List<SchemeUse>, denials: Map<String, Authorization.Denied>): List<String> {
        val bearer = uses.firstOrNull { use -> use.source == CredentialSource.Bearer }
        val isBasic = uses.any { use -> use.source == CredentialSource.Basic }
        val denial = bearer?.let { use -> denials[use.scheme] }
        val bearerChallenge = bearer?.let { bearerOf(denial) }
        val basicChallenge = """Basic realm="$realm", charset="UTF-8"""".takeIf { isBasic }
        return listOfNotNull(bearerChallenge, basicChallenge)
    }

    fun forbidden(use: SchemeUse, forbidden: Authorization.Forbidden): List<String> {
        val isBearer = use.source == CredentialSource.Bearer
        val scopes = forbidden.scopes.filter(::isScopeToken).joinToString(" ")
        val scope = """, scope="$scopes"""".takeIf { scopes.isNotEmpty() }.orEmpty()
        val challenge = """Bearer realm="$realm", error="insufficient_scope"$scope"""
        return listOfNotNull(challenge.takeIf { isBearer })
    }

    private fun bearerOf(denial: Authorization.Denied?): String {
        val error = denial?.error?.takeIf(::isScopeToken)
        val description = denial?.description?.filter(::isDescriptionChar)
        val errorPart = error?.let { code -> """, error="$code"""" }.orEmpty()
        val descriptionPart = description?.takeIf { error != null }?.let { text -> """, error_description="$text"""" }
        return """Bearer realm="$realm"$errorPart${descriptionPart.orEmpty()}"""
    }

    private fun isScopeToken(text: String): Boolean = text.isNotEmpty() && text.all(::isScopeChar)

    private fun isScopeChar(char: Char): Boolean = char == '!' || char in '#'..'[' || char in ']'..'~'

    private fun isDescriptionChar(char: Char): Boolean = char in ' '..'!' || char in '#'..'[' || char in ']'..'~'
}
