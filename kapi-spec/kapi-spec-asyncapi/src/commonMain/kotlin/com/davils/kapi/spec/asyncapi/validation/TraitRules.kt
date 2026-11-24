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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable

private const val FIELDS_PER_TRAIT: Long = 1L

internal object TraitRules {
    fun check(scope: RuleScope) {
        val operations = scope.all<Operation>().sumOf { operation -> inherited(scope, operation.traits) }
        val messages = scope.all<Message>().sumOf { message -> inherited(scope, message.traits) }
        val total = operations + messages
        val maximum = scope.limits.maximumNodes
        if (total <= maximum) return
        val message = "Applying the traits copies $total extensions and fields, more than the $maximum nodes a " +
            "document may have"
        scope.report(DiagnosticCodes.TRAIT_TOO_LARGE, message, scope.document)
    }

    private inline fun <reified T : Element> inherited(scope: RuleScope, traits: List<Referable<T>>): Long =
        traits.sumOf { trait ->
            val extensions = scope.resolve(trait)?.run { extensions.entries }
            extensions.orEmpty().size + FIELDS_PER_TRAIT
        }
}
