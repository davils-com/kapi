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
import com.davils.kapi.spec.openapi.VersionFamily

internal object DocumentRules {
    private val needs: Map<VersionFamily, String> = mapOf(
        VersionFamily.V30 to "the field paths",
        VersionFamily.V31 to "at least one of the fields paths, components and webhooks",
        VersionFamily.V32 to "at least one of the fields paths, components and webhooks",
    )

    private val rules: List<(RuleScope) -> Unit> = listOf(
        ::content,
        OperationRules::check,
        PathRules::check,
        MethodRules::check,
        ParameterRules::check,
        SecurityRules::check,
        SchemaRules::check,
        PatternRules::check,
        ServerRules::check,
        ContentRules::check,
        ComponentRules::check,
    )

    fun check(scope: RuleScope) {
        rules.forEach { rule -> rule(scope) }
    }

    private fun content(scope: RuleScope) {
        val document = scope.document
        val hasMore = document.components != null || document.webhooks.isNotEmpty()
        val isEnough = hasMore && scope.family.isAtLeast(VersionFamily.V31)
        if (document.paths != null || isEnough) return
        val needed = needs.getValue(scope.family)
        scope.report(DiagnosticCodes.FIELD_MISSING, "The document needs $needed", document)
    }
}
