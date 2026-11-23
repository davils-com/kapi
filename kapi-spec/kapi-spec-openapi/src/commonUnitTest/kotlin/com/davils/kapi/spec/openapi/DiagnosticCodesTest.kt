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

package com.davils.kapi.spec.openapi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class DiagnosticCodesTest : FunSpec({
    context("the catalogue") {
        test("keeps every code in the namespace of the document") {
            DiagnosticCodes.all.forEach { code -> code.value shouldStartWith "openapi." }
        }

        test("names no code twice") {
            DiagnosticCodes.all.shouldNotContainDuplicates()
        }

        test("lists the codes it declares") {
            DiagnosticCodes.all shouldContain DiagnosticCodes.FIELD_UNKNOWN
            DiagnosticCodes.all shouldContain DiagnosticCodes.MEDIA_TYPE_INVALID
            DiagnosticCodes.FIELD_UNKNOWN.value shouldBe "openapi.field.unknown"
        }
    }
})
