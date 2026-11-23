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

package com.davils.kapi.spec.openapi.model.schema

import com.davils.kapi.spec.core.value.NumberValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val one: NumberValue = NumberValue.of(1L)

class BoundTest : FunSpec({
    context("a bound") {
        test("is inclusive unless it says otherwise") {
            Bound(one).isExclusive shouldBe false
        }

        test("equals the bound of the same value and exclusiveness, however the value was written") {
            val bound = Bound(one, isExclusive = true)

            bound shouldBe Bound(checkNotNull(NumberValue.parseOrNull("1.0")), isExclusive = true)
            bound.hashCode() shouldBe Bound(one, isExclusive = true).hashCode()
            bound shouldNotBe Bound(one)
            bound shouldNotBe Bound(NumberValue.of(2L), isExclusive = true)
            val text: Any = "1"
            (bound == text) shouldBe false
        }

        test("shows its value and whether it is exclusive") {
            Bound(one, isExclusive = true).toString() shouldBe "Bound(1, isExclusive=true)"
        }
    }
})
