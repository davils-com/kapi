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

package com.davils.kapi.runtime.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FieldLimitsTest : FunSpec({
    test("are built with their defaults and show them") {
        fieldLimits().toString() shouldBe "FieldLimits(fieldLength=8192, members=64, parameters=32, ranges=16)"
        fieldLimits().maxFieldLength shouldBe FieldLimits.Default.maxFieldLength
    }
})
