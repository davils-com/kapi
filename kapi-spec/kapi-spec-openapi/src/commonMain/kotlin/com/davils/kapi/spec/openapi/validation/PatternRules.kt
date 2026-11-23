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
import com.davils.kapi.spec.schema.pattern.Pattern
import com.davils.kapi.spec.schema.pattern.PatternCompilation
import com.davils.kapi.spec.schema.pattern.PatternLimits
import com.davils.kapi.spec.schema.pattern.patternLimits

internal object PatternRules {
    private const val INVALID: String = "A pattern is no regular expression of ECMA-262"

    private const val UNSUPPORTED: String =
        "A pattern needs what kapi cannot match in linear time, so no value is checked against it"

    fun check(scope: RuleScope) {
        val limits = patternLimits { maximumSourceLength = scope.limits.maximumPatternLength }
        scope.all<SchemaDefinition>().forEach { schema ->
            val patterns = listOfNotNull(schema.pattern) + schema.patternProperties.keys
            patterns.forEach { pattern -> pattern(scope = scope, schema = schema, pattern = pattern, limits = limits) }
        }
    }

    private fun pattern(scope: RuleScope, schema: SchemaDefinition, pattern: String, limits: PatternLimits) {
        val limit = limits.maximumSourceLength
        if (pattern.length > limit) {
            val message = "A pattern of ${pattern.length} characters is longer than the $limit a reading accepts"
            return scope.report(DiagnosticCodes.LIMIT_PATTERN_LENGTH, message, schema)
        }
        compiled(scope = scope, schema = schema, pattern = pattern, limits = limits)
    }

    private fun compiled(scope: RuleScope, schema: SchemaDefinition, pattern: String, limits: PatternLimits) {
        val refused = Pattern.compile(pattern, limits) as? PatternCompilation.Refused ?: return
        val isInvalid = !refused.reason.isValidSyntax
        val invalid = DiagnosticCodes.SCHEMA_PATTERN_INVALID.takeIf { isInvalid }
        val reason = INVALID.takeIf { isInvalid } ?: UNSUPPORTED
        scope.report(invalid ?: DiagnosticCodes.SCHEMA_PATTERN_UNSUPPORTED, "$reason: ${refused.message}", schema)
    }
}
