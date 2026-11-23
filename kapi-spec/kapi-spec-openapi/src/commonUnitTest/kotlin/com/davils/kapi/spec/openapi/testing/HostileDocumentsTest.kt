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
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.core.policy.ReadLimitsBuilder
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import com.davils.kapi.spec.testing.HostileDocuments
import com.davils.kapi.spec.testing.Corpus

private const val BOMB_LEVELS: Int = 12

private const val DEEP: Int = 5_000

private const val MANY: Int = 5_000

private const val LONG: Int = 200

private suspend fun reading(text: String): Reading = OpenApi.reader().read(Source.Text(text))

private fun Reading.codes(): List<ViolationCode> = diagnostics.map(Diagnostic::code)

class HostileDocumentsTest : FunSpec({
    context("a hostile document") {
        test("cannot expand aliases beyond the budget of a reading") {
            val reading = reading(HostileDocuments.aliasBomb(BOMB_LEVELS))

            reading.shouldBeInstanceOf<Reading.Malformed>()
            reading.codes() shouldContain ViolationCode("kaml.limit.alias-expansion")
        }

        test("cannot nest deeper than a reading follows") {
            val reading = reading(HostileDocuments.deepNesting(DEEP))

            reading.shouldBeInstanceOf<Reading.Malformed>()
            reading.codes() shouldContain ViolationCode("kaml.limit.depth")
        }

        test("cannot flood a reading with findings") {
            val reading = reading(HostileDocuments.unknownFields(MANY))

            reading.shouldBeInstanceOf<Reading.Invalid>()
            reading.diagnostics.size shouldBe ReadLimitsBuilder.DEFAULT_DIAGNOSTICS + 1
            reading.codes().last() shouldBe DiagnosticCodes.LIMIT_DIAGNOSTICS
        }

        test("cannot send a reading through an endless chain of references") {
            val reading = reading(HostileDocuments.referenceChain(LONG))

            reading.shouldBeInstanceOf<Reading.Invalid>()
            reading.codes().toSet() shouldBe setOf(DiagnosticCodes.REFERENCE_CHAIN_TOO_LONG)
        }
    }

    context("a reading") {
        test("gives the same result for the same bytes, finding for finding") {
            val text = HostileDocuments.unknownFields(LONG)

            reading(text) shouldBe reading(text)
            Corpus.openApiNames.forEach { name ->
                val source = Corpus.openApi(name)
                val reader = OpenApi.reader()

                reader.read(source) shouldBe reader.read(source)
            }
        }
    }
})
