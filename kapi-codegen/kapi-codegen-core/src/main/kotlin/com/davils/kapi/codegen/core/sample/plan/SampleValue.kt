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

package com.davils.kapi.codegen.core.sample.plan

import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.sample.SampleNumbers
import com.davils.kapi.codegen.core.sample.SampleRules
import java.math.BigDecimal

internal sealed interface SampleValue {
    fun meets(rules: List<RulePlan>): Boolean = true

    data class Text(val value: String) : SampleValue {
        override fun meets(rules: List<RulePlan>): Boolean = SampleRules.text(value, rules)
    }

    data class Number(val value: BigDecimal, val kind: ScalarKind) : SampleValue {
        override fun meets(rules: List<RulePlan>): Boolean =
            SampleRules.number(SampleNumbers.seen(value, kind), kind, rules)
    }

    data class Flag(val isTrue: Boolean) : SampleValue

    data class Moment(val text: String) : SampleValue

    data object Bytes : SampleValue

    data object Json : SampleValue

    data object Null : SampleValue

    data class Entry(val type: String, val name: String) : SampleValue

    data class Model(val name: String) : SampleValue

    data class Items(val values: List<SampleValue>) : SampleValue {
        override fun meets(rules: List<RulePlan>): Boolean {
            val isSized = SampleRules.items(values.size, rules)
            val isDistinct = RulePlan.Unique !in rules || values.toSet().size == values.size
            return isSized && isDistinct
        }
    }

    data class Entries(val values: Map<String, SampleValue>) : SampleValue {
        override fun meets(rules: List<RulePlan>): Boolean = SampleRules.entries(values.size, rules)
    }

    data class Built(val type: String, val arguments: Map<String, SampleValue>) : SampleValue

    data class Stream(val values: List<SampleValue>) : SampleValue
}
