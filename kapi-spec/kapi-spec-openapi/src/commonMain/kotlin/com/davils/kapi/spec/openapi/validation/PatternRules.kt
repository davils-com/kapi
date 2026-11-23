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
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition

internal object PatternRules {
    private val nestedQuantifier: Regex = Regex("""\((?:[^()\\]|\\.)*[+*](?:[^()\\]|\\.)*\)(?:[+*]|\{\d*,\d*})""")

    fun check(scope: RuleScope) {
        scope.all<SchemaDefinition>().forEach { schema ->
            val patterns = listOfNotNull(schema.pattern) + schema.patternProperties.keys
            patterns.forEach { pattern -> pattern(scope, schema, pattern) }
        }
    }

    private fun pattern(scope: RuleScope, schema: SchemaDefinition, pattern: String) {
        val limit = scope.limits.maximumPatternLength
        if (pattern.length > limit) {
            val message = "A pattern of ${pattern.length} characters is longer than the $limit a reading accepts"
            return scope.report(DiagnosticCodes.LIMIT_PATTERN_LENGTH, message, schema)
        }
        compiled(scope, schema, pattern)
    }

    private fun compiled(scope: RuleScope, schema: SchemaDefinition, pattern: String) {
        val problem = problemOf(pattern)
        if (problem != null) {
            val message = "The pattern '$pattern' is no regular expression Kotlin can compile: $problem"
            return scope.report(DiagnosticCodes.SCHEMA_PATTERN_INVALID, message, schema)
        }
        if (!nestedQuantifier.containsMatchIn(pattern)) return
        val message = "The pattern '$pattern' repeats a repeated group, which can take very long on crafted input"
        scope.report(DiagnosticCodes.SCHEMA_PATTERN_BACKTRACKING, message, schema)
    }

    private fun problemOf(pattern: String): String? = try {
        Regex(pattern)
        null
    } catch (failure: IllegalArgumentException) {
        failure.message.orEmpty()
    }
}
