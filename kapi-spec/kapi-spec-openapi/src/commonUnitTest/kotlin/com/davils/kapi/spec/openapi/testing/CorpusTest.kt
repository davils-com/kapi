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

package com.davils.kapi.spec.openapi.testing

import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.testing.Corpus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val UNDECLARED_LINK: String = "response-links"

private const val HOSTILE: String = "hostile"

private const val COMPONENT_NAME: String = "[openapi.component.name]"

class CorpusTest : FunSpec({
    context("the corpus") {
        test("is read without a finding, but for two documents that break a rule on purpose") {
            val reader = OpenApi.reader()
            val findings = Corpus.openApiNames.associateWith { name ->
                val reading = reader.read(Corpus.openApi(name))
                reading.diagnostics.map(Diagnostic::toString)
            }

            val names = findings.getValue(HOSTILE).filterNot { finding -> COMPONENT_NAME in finding }

            val flagged = findings.filterValues { diagnostics -> diagnostics.isNotEmpty() }

            flagged.keys shouldBe setOf(HOSTILE, UNDECLARED_LINK)
            names shouldBe emptyList()
            findings.getValue(UNDECLARED_LINK).single() shouldContain "[openapi.link.target]"
        }

        test("names every document once and places it under its own scheme") {
            val petstore = Corpus.openApi("petstore")

            Corpus.openApiNames.shouldNotContainDuplicates()
            petstore.uri shouldBe DocumentUri("corpus:/petstore.yaml")
            petstore.text shouldContain "openapi: 3.1.0"
        }

        test("refuses to hand out a document it does not hold") {
            val failure = shouldThrow<IllegalArgumentException> { Corpus.openApiText("missing") }

            failure.message shouldContain "holds no OpenAPI document named missing"
        }
    }
})
