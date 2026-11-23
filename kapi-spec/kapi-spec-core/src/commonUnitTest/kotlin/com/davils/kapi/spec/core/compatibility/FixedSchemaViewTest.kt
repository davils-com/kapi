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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.core.compatibility

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FixedSchemaViewTest : FunSpec({
    context("the view of a boolean schema") {
        test("has no keywords and leads nowhere") {
            val view = FixedSchemaView(location = at("/components/schemas/Any"), isAccepting = true)
            val nothing = listOf(
                view.target,
                view.opaque,
                view.types,
                view.format,
                view.pattern,
                view.enumeration,
                view.constant,
                view.minLength,
                view.maxLength,
                view.minimum,
                view.maximum,
                view.multipleOf,
                view.minItems,
                view.maxItems,
                view.minProperties,
                view.maxProperties,
                view.additionalProperties,
                view.items,
                view.discriminator,
            )
            val flags = listOf(
                view.isUnresolved,
                view.isMinimumExclusive,
                view.isMaximumExclusive,
                view.hasUniqueItems,
                view.isDeprecated,
                view.isReadOnly,
                view.isWriteOnly,
            )
            val empty = listOf(view.properties.keys, view.required, view.allOf, view.anyOf, view.oneOf)

            nothing.all { value -> value == null } shouldBe true
            flags.none { flag -> flag } shouldBe true
            empty.all(Collection<Any>::isEmpty) shouldBe true
            view.isAccepting shouldBe true
            view.dereferenced(maximumHops = 1) shouldBe view
        }
    }
})
