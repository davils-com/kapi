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
import com.davils.kapi.spec.openapi.model.component.Components

internal object ComponentRules {
    private val name: Regex = Regex("[a-zA-Z0-9._-]+")

    fun check(scope: RuleScope) {
        scope.all<Components>().forEach { components ->
            val named = namesOf(components).filterNot { key -> name.matches(key) }
            named.forEach { key ->
                val message = "The component name '$key' may only hold letters, digits, dots, hyphens and underscores"
                scope.report(DiagnosticCodes.COMPONENT_NAME, message, components)
            }
        }
    }

    private fun namesOf(components: Components): List<String> {
        val sections = listOf(
            components.schemas,
            components.responses,
            components.parameters,
            components.examples,
            components.requestBodies,
            components.headers,
            components.securitySchemes,
            components.links,
            components.callbacks,
            components.pathItems,
            components.mediaTypes,
        )
        return sections.flatMap { section -> section.keys }
    }
}
