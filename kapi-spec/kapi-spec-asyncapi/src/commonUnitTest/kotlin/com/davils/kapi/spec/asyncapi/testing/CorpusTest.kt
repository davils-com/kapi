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

package com.davils.kapi.spec.asyncapi.testing

import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.asyncapi.fixture.document
import com.davils.kapi.spec.asyncapi.fixture.rewritten
import com.davils.kapi.spec.core.diagnostic.render
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class CorpusTest : FunSpec({
    context("every document of the corpus") {
        Corpus.names.forEach { name ->
            test("reads $name without a finding") {
                val reading = AsyncApi.reader().read(Corpus.source(name))

                reading.diagnostics.render() shouldBe ""
                reading.diagnostics.shouldBeEmpty()
            }

            test("writes $name so that it reads back to the same document") {
                val reader = AsyncApi.reader()
                val document = reader.read(Corpus.source(name)).document()

                document.rewritten() shouldBe document
            }
        }
    }
})
