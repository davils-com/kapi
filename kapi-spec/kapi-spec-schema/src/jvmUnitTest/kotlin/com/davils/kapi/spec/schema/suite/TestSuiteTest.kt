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

package com.davils.kapi.spec.schema.suite

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TestSuiteTest : FunSpec({
    SuiteDraft.entries.forEach { draft ->
        context(draft.folder) {
            val compiler = TestSuite.compiler(draft)
            TestSuite.files(draft).forEach { file ->
                test(TestSuite.nameOf(draft, file)) {
                    val outcome = TestSuite.run(compiler, draft, file)
                    withClue(outcome.unexpected.joinToString("\n")) { outcome.unexpected.size shouldBe 0 }
                    withClue(outcome.staleSkips.joinToString("\n")) { outcome.staleSkips.size shouldBe 0 }
                }
            }
        }
    }
})
