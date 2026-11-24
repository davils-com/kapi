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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.model.plan.LimitKind
import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

private val text: TypeUse =
    TypeUse(TypeShape.Scalar(ScalarKind.Text), rules = listOf(RulePlan.Limit(LimitKind.MinLength, 1)))

class ValueRulesTest : FunSpec({
    context("the rules of a value read on its own") {
        test("check a map as a whole and each of its values") {
            val map = TypeUse(TypeShape.Entries(text), rules = listOf(RulePlan.Limit(LimitKind.MaxProperties, 2)))

            val validation = ValidationCode(PatternTable())
            val code = validation.value(map, "tags").toString()

            code shouldContain "if (it.size > 2) fail(\"must have at most 2 entries\""
            code shouldContain "it.values.forEach { item1 ->"
        }
    }
})
