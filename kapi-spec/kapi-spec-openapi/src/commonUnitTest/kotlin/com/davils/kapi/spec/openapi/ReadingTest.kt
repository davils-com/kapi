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

import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.spec.openapi.model.info.InfoBuilder
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.io.bytestring.ByteString

private val diagnostic: Diagnostic = Diagnostic(DiagnosticCodes.FIELD_UNKNOWN, Severity.Warning, "unknown")

private val source: SourceDocument = SourceDocument(DocumentUri.MEMORY, 1, ByteString(1))

private fun document(title: String): Document {
    val info = InfoBuilder()
    info.title = title
    info.version = "1.0.0"
    val builder = DocumentBuilder()
    builder.info = info.build()
    return builder.build()
}

private val pets: Document = document("Pets")

private val toys: Document = document("Toys")

class ReadingTest : FunSpec({
    context("a reading that read the document") {
        test("equals the reading of the same document, diagnostics and sources") {
            val read = Reading.Read(pets, listOf(diagnostic), listOf(source))

            read shouldBe Reading.Read(document("Pets"), listOf(diagnostic), listOf(source))
            read.hashCode() shouldBe Reading.Read(pets, listOf(diagnostic), listOf(source)).hashCode()
            read shouldNotBe Reading.Read(toys, listOf(diagnostic), listOf(source))
            read shouldNotBe Reading.Read(pets, emptyList(), listOf(source))
            read shouldNotBe Reading.Read(pets, listOf(diagnostic), emptyList())
            (read as Reading == Reading.Invalid(pets, listOf(diagnostic), listOf(source))) shouldBe false
            read.toString() shouldBe "Read(1 diagnostics, 1 sources)"
        }
    }

    context("a reading that found errors") {
        test("equals the reading of the same partial document, diagnostics and sources") {
            val invalid = Reading.Invalid(pets, listOf(diagnostic), listOf(source))

            invalid shouldBe Reading.Invalid(document("Pets"), listOf(diagnostic), listOf(source))
            invalid.hashCode() shouldBe Reading.Invalid(pets, listOf(diagnostic), listOf(source)).hashCode()
            invalid shouldNotBe Reading.Invalid(toys, listOf(diagnostic), listOf(source))
            invalid shouldNotBe Reading.Invalid(pets, emptyList(), listOf(source))
            invalid shouldNotBe Reading.Invalid(pets, listOf(diagnostic), emptyList())
            (invalid as Reading == Reading.Malformed(listOf(diagnostic), listOf(source))) shouldBe false
            invalid.toString() shouldBe "Invalid(1 diagnostics, 1 sources)"
        }
    }

    context("a reading of no document") {
        test("equals the reading of the same diagnostics and sources") {
            val malformed = Reading.Malformed(listOf(diagnostic), listOf(source))

            malformed shouldBe Reading.Malformed(listOf(diagnostic), listOf(source))
            malformed.hashCode() shouldBe Reading.Malformed(listOf(diagnostic), listOf(source)).hashCode()
            malformed shouldNotBe Reading.Malformed(emptyList(), listOf(source))
            malformed shouldNotBe Reading.Malformed(listOf(diagnostic), emptyList())
            (malformed as Reading == Reading.Read(pets, listOf(diagnostic), listOf(source))) shouldBe false
            malformed.toString() shouldBe "Malformed(1 diagnostics, 1 sources)"
        }
    }
})
