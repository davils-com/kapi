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

package com.davils.kapi.spec.core.source

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val CONTENT: String = "openapi: 3.1.0"

private const val HOSTILE_REASON: String = "the disk\u001b[2J is gone"

private const val SHA_256_LENGTH: Int = 32

private val petstore: DocumentUri = DocumentUri("memory:/petstore.yaml")

private val digest: ByteString = ByteString(ByteArray(SHA_256_LENGTH))

class ResolutionTest : FunSpec({
    context("a found document") {
        test("equals the document of the same bytes and shows only its size") {
            val found = Resolution.Found(CONTENT.encodeToByteString())

            found shouldBe Resolution.Found(CONTENT.encodeToByteString())
            found.hashCode() shouldBe CONTENT.encodeToByteString().hashCode()
            found shouldNotBe Resolution.Found("asyncapi: 3.0.0".encodeToByteString())
            (found as Resolution == Resolution.Missing) shouldBe false
            found.toString() shouldBe "Found(${CONTENT.length} bytes)"
        }
    }

    context("a refusal") {
        test("equals the refusal of the same reason") {
            val refused = Resolution.Refused(Refusal.Size)

            refused shouldBe Resolution.Refused(Refusal.Size)
            refused.hashCode() shouldBe Refusal.Size.hashCode()
            refused shouldNotBe Resolution.Refused(Refusal.Confinement)
            (refused as Resolution == Resolution.Failed("Size")) shouldBe false
            refused.toString() shouldBe "Refused(Size)"
        }
    }

    context("a failure") {
        test("equals the failure of the same reason and shows the reason escaped") {
            val failed = Resolution.Failed(HOSTILE_REASON)

            failed shouldBe Resolution.Failed(HOSTILE_REASON)
            failed.hashCode() shouldBe HOSTILE_REASON.hashCode()
            failed shouldNotBe Resolution.Failed("the disk is gone")
            (failed as Resolution == Resolution.Missing) shouldBe false
            failed.toString() shouldNotContain "\u001b"
        }
    }

    context("a missing document") {
        test("is one value") {
            Resolution.Missing.toString() shouldBe "Missing"
        }
    }

    context("a source document") {
        test("equals the document of the same address, length and digest") {
            val document = SourceDocument(petstore, CONTENT.length, digest)

            document shouldBe SourceDocument(petstore, CONTENT.length, digest)
            document.hashCode() shouldBe SourceDocument(petstore, CONTENT.length, digest).hashCode()
            document shouldNotBe SourceDocument(DocumentUri.MEMORY, CONTENT.length, digest)
            document shouldNotBe SourceDocument(petstore, CONTENT.length + 1, digest)
            document shouldNotBe SourceDocument(petstore, CONTENT.length, ByteString(1))
            val text: Any = CONTENT
            (document == text) shouldBe false
            document.toString() shouldBe "SourceDocument(memory:/petstore.yaml, ${CONTENT.length} bytes)"
        }
    }
})
