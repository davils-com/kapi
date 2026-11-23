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

package com.davils.kapi.spec.core.policy

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.fixture.NodeBuilder
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.Extensions
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

@OptIn(DavilsInternal::class)
class BuilderClosingTest : FunSpec({
    context("a builder that was evaluated") {
        test("refuses every change of a policy or a limit instead of losing it") {
            val limits = ReadLimitsBuilder()
            val references = ReferencePolicyBuilder()
            val severities = SeverityPolicyBuilder()
            limits.build()
            references.build()
            severities.build()

            shouldThrow<IllegalStateException> { limits.maximumDepth = 1 }
            shouldThrow<IllegalStateException> { references.isExternalAllowed = false }
            shouldThrow<IllegalStateException> { severities.areWarningsErrors = true }
            shouldThrow<IllegalStateException> { severities.downgrade(ViolationCode("kapi.test.code"), "known") }
        }

        test("refuses every change of an element instead of losing it") {
            val element = NodeBuilder()
            val vendor = ExtensionName("x-vendor")
            element.build()

            shouldThrow<IllegalStateException> {
                element.location = Location(DocumentUri("memory:/api.yaml"), JsonPointer.of("paths"))
            }
            shouldThrow<IllegalStateException> { element.extension(vendor, Value.Text("late")) }
            shouldThrow<IllegalStateException> { element.adopt(Extensions(mapOf(vendor to Value.Text("late")))) }
        }
    }
})
