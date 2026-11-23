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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem

private const val SPECIALS: String = "!#\$%&'*+-.^_`|~"

internal object MethodRules {
    private val fixed: Set<String> = Method.entries.map(Method::keyword).toSet()

    fun check(scope: RuleScope) {
        scope.all<PathItem>().forEach { item ->
            item.additionalOperations.forEach { (name, operation) -> method(scope, name, operation) }
        }
    }

    private fun method(scope: RuleScope, name: String, operation: Operation) {
        if (!isToken(name)) {
            val message = "'$name' is no method token, which holds letters, digits and the characters $SPECIALS"
            return scope.report(DiagnosticCodes.PATH_METHOD_TOKEN, message, operation)
        }
        if (name.lowercase() !in fixed) return
        val message = "The method $name has a field of its own in a path item and is declared there"
        scope.report(DiagnosticCodes.PATH_METHOD_FIXED, message, operation)
    }

    private fun isToken(name: String): Boolean = name.isNotEmpty() && name.all(::isTokenCharacter)

    private fun isTokenCharacter(character: Char): Boolean {
        val isLetter = character in 'a'..'z' || character in 'A'..'Z'
        return isLetter || character in '0'..'9' || character in SPECIALS
    }
}
