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

package com.davils.kapi.spec.schema

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.json.JsonObject

class BuilderClosingTest : FunSpec({
    context("a builder that was evaluated") {
        test("refuses every change of a compiler or its limits instead of losing it") {
            val compiler = CompilerBuilder()
            val limits = SchemaLimitsBuilder()
            compiler.build()
            limits.build()

            shouldThrow<IllegalStateException> { compiler.isFormatAsserted = true }
            shouldThrow<IllegalStateException> { compiler.limits { maximumSchemaDepth = 1 } }
            val late = DocumentUri("memory:/late.json")

            shouldThrow<IllegalStateException> { compiler.schema(late, JsonObject(emptyMap())) }
            shouldThrow<IllegalStateException> { limits.maximumSubschemas = 1 }
        }
    }
})
