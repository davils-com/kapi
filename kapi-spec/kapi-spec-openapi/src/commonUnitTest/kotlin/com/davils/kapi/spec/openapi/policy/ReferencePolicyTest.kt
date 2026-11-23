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

package com.davils.kapi.spec.openapi.policy

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.policy.PolicyCodes
import com.davils.kapi.spec.core.policy.ReferencePolicy
import com.davils.kapi.spec.core.policy.ReferencePolicyBuilder
import com.davils.kapi.spec.openapi.fixture.findings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ReferencePolicyTest : FunSpec({
    context("the default policy") {
        test("confines references to the folder of the document and allows other documents in it") {
            val policy = ReferencePolicy.Default

            policy.root shouldBe null
            policy.isExternalAllowed shouldBe true
            policy.toString() shouldBe
                "ReferencePolicy(root=the folder of the document, isExternalAllowed=true)"
        }
    }

    context("a root") {
        test("names the directory references may not leave") {
            val builder = ReferencePolicyBuilder()
            builder.root = DocumentUri("file:///api/")
            builder.isExternalAllowed = false
            val policy = builder.build()

            policy.root shouldBe DocumentUri("file:///api/")
            policy.isExternalAllowed shouldBe false
            policy.toString() shouldContain "root=file:///api/"
        }

        test("is refused when it names a file") {
            val builder = ReferencePolicyBuilder()
            builder.root = DocumentUri("file:///api/petstore.yaml")

            builder.validate().findings() shouldBe listOf("root" to PolicyCodes.ROOT_NOT_DIRECTORY)
        }
    }
})
