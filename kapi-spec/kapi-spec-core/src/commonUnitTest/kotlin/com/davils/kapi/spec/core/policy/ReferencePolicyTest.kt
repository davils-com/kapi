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

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private fun codesOf(root: DocumentUri?): List<String> {
    val builder = ReferencePolicyBuilder()
    builder.root = root
    return builder.validate().errors.map { violation -> violation.code.value }
}

class ReferencePolicyTest : FunSpec({
    context("the default policy") {
        test("confines references to the folder of the document and allows other documents") {
            val policy = ReferencePolicy.Default

            policy.root shouldBe null
            policy.isExternalAllowed shouldBe true
            policy.toString() shouldBe "ReferencePolicy(root=the folder of the document, isExternalAllowed=true)"
        }
    }

    context("a root") {
        test("is a directory that ends with a slash") {
            codesOf(DocumentUri("file:///work/")) shouldBe emptyList()
            codesOf(null) shouldBe emptyList()
        }

        test("is refused when it names a file") {
            val codes = codesOf(DocumentUri("file:///work/api.yaml"))

            codes shouldBe listOf(PolicyCodes.ROOT_NOT_DIRECTORY.value)
        }

        test("is shown by the policy") {
            val builder = ReferencePolicyBuilder()
            builder.root = DocumentUri("file:///work/")
            builder.isExternalAllowed = false

            val text = builder.build().toString()

            text shouldContain "root=file:///work/"
            text shouldContain "isExternalAllowed=false"
        }
    }
})
