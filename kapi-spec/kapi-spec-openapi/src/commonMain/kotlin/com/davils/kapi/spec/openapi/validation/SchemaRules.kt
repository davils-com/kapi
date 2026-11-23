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

import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.schema.Bound
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.resolve

private const val SCHEMA_POINTER: String = "#/components/schemas/"

internal object SchemaRules {
    private val nothing: NumberValue = NumberValue.of(0L)

    private val local: Regex = Regex("[^#/]+")

    fun check(scope: RuleScope) {
        scope.all<SchemaDefinition>().forEach { schema ->
            constraints(scope, schema)
            mapping(scope, schema)
            cycle(scope, schema)
        }
    }

    private fun constraints(scope: RuleScope, schema: SchemaDefinition) {
        val contradictions = listOf(
            "minimum is larger than maximum" to isEmptyRange(schema.minimum, schema.maximum),
            "minLength is larger than maxLength" to isReversed(schema.minLength, schema.maxLength),
            "minItems is larger than maxItems" to isReversed(schema.minItems, schema.maxItems),
            "minProperties is larger than maxProperties" to isReversed(schema.minProperties, schema.maxProperties),
            "minContains is larger than maxContains" to isReversed(schema.minContains, schema.maxContains),
            "multipleOf is not larger than zero" to (schema.multipleOf?.let { factor -> factor <= nothing } == true),
        )
        contradictions.filter { (_, isBroken) -> isBroken }.forEach { (reason, _) ->
            val message = "No value fits the schema, because $reason"
            scope.report(DiagnosticCodes.SCHEMA_CONSTRAINT, message, schema)
        }
    }

    private fun isReversed(minimum: Int?, maximum: Int?): Boolean =
        minimum != null && maximum != null && minimum > maximum

    private fun isEmptyRange(minimum: Bound?, maximum: Bound?): Boolean {
        if (minimum == null || maximum == null) return false
        val order = minimum.value.compareTo(maximum.value)
        val isOpen = minimum.isExclusive || maximum.isExclusive
        val isTouching = order == 0 && isOpen
        return order > 0 || isTouching
    }

    private fun mapping(scope: RuleScope, schema: SchemaDefinition) {
        val components = scope.document.components
        val known = components?.schemas.orEmpty().keys
        val values = schema.discriminator?.mapping.orEmpty()
        val names = values.mapValues { (_, target) -> target.removePrefix(SCHEMA_POINTER) }
        val unknown = names.filterValues { name -> local.matches(name) && name !in known }
        unknown.keys.forEach { value ->
            val target = values.getValue(value)
            val message = "The discriminator maps $value to $target, which is no schema of the components"
            scope.report(DiagnosticCodes.SCHEMA_DISCRIMINATOR_MAPPING, message, schema)
        }
    }

    private fun cycle(scope: RuleScope, schema: SchemaDefinition) {
        val limit = scope.limits.maximumReferenceHops
        val chain = generateSequence(schema) { current -> next(scope, current) }
        val hops = chain.take(limit + 1).toList()
        if (hops.size <= limit) return
        val message = "The schema refers to nothing but references that lead back to it or never end"
        scope.report(DiagnosticCodes.REFERENCE_CYCLE, message, schema)
    }

    private fun next(scope: RuleScope, schema: SchemaDefinition): SchemaDefinition? {
        val reference = schema.reference ?: return null
        return scope.document.resolve(reference) as? SchemaDefinition
    }
}
