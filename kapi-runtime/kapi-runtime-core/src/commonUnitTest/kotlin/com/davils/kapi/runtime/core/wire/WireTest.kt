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

package com.davils.kapi.runtime.core.wire

import com.davils.kapi.runtime.core.wire.fixture.NotePatch
import com.davils.kapi.runtime.core.wire.fixture.Photo
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val SET: String = """{"text":"hello"}"""

private const val CLEARED: String = """{"text":null}"""

private const val UNTOUCHED: String = "{}"

private const val HELLO: String = "hello"

private const val NAME: String = "photo.png"

private const val PNG: String = "image/png"

private const val ENCODED: String = """{"thumbnail":"aGk=","file":"aGVsbG8="}"""

private val terse: Json = Json { encodeDefaults = false }

private val eager: Json = Json { encodeDefaults = true }

private val hello: ByteString = HELLO.encodeToByteString()

private fun named(filename: String?): FilePart = FilePart(hello, filename)

class WireTest : FunSpec({
    context("a patch") {
        test("sets a value and clears for null") {
            Patch.of(HELLO) shouldBe Patch.Value(HELLO)
            Patch.of<String>(null) shouldBe Patch.Null
            Patch.Value(HELLO).hashCode() shouldBe HELLO.hashCode()
            Patch.Value("a\nb").toString() shouldBe "Value(a\\nb)"
            listOf(Patch.Absent, Patch.Null).map(Patch<String>::toString) shouldBe listOf("Absent", "Null")
        }

        test("is written as its value, as null or left out") {
            terse.encodeToString(NotePatch.serializer(), NotePatch(Patch.Value(HELLO))) shouldBe SET
            terse.encodeToString(NotePatch.serializer(), NotePatch(Patch.Null)) shouldBe CLEARED
            terse.encodeToString(NotePatch.serializer(), NotePatch()) shouldBe UNTOUCHED
        }

        test("is read as set, cleared or absent") {
            terse.decodeFromString(NotePatch.serializer(), SET).text shouldBe Patch.Value(HELLO)
            terse.decodeFromString(NotePatch.serializer(), CLEARED).text shouldBe Patch.Null
            terse.decodeFromString(NotePatch.serializer(), UNTOUCHED).text shouldBe Patch.Absent
        }

        test("cannot be written when it is absent and not left out") {
            val failure = shouldThrow<SerializationException> {
                eager.encodeToString(NotePatch.serializer(), NotePatch())
            }

            failure.message shouldContain "is left out and cannot be written"
        }
    }

    context("a file part") {
        test("keeps its content, its name, its type and a copy of its headers") {
            val headers = mutableMapOf("X-Origin" to listOf("camera"))
            val part = FilePart(content = hello, filename = NAME, contentType = PNG, headers = headers)

            headers.clear()

            part.content shouldBe hello
            part.filename shouldBe NAME
            part.contentType shouldBe PNG
            part.headers shouldBe mapOf("X-Origin" to listOf("camera"))
        }

        test("equals a part of the same content and description") {
            named(NAME) shouldBe named(NAME)
            named(NAME).hashCode() shouldBe named(NAME).hashCode()
            named(NAME) shouldNotBe named("other.png")
            named(NAME).equals(NAME) shouldBe false
        }

        test("renders its size and never its content, and escapes its name") {
            named(NAME).toString() shouldBe "FilePart(5 bytes, filename=photo.png, contentType=unnamed)"
            named(null).toString() shouldBe "FilePart(5 bytes, filename=unnamed, contentType=unnamed)"
            named("a\u001b[31m.png").toString() shouldNotContain "\u001b"
            FilePart(hello, contentType = PNG).toString() shouldContain "contentType=$PNG"
        }

        test("has a safe name that is the last segment of whatever path was sent") {
            named("/etc/photo.png").safeFilename shouldBe NAME
            named("""C:\Users\me\photo.png""").safeFilename shouldBe NAME
            named("../../photo.png").safeFilename shouldBe NAME
            named(" photo.png ").safeFilename shouldBe NAME
            named("pho\u0000to.png\n").safeFilename shouldBe NAME
            listOf(null, "", "/", "..", "a/.", " ").forEach { filename -> named(filename).safeFilename shouldBe null }
        }
    }

    context("bytes in a document") {
        test("are written and read as Base64") {
            val photo = Photo("hi".encodeToByteString(), FilePart(hello))

            Json.encodeToString(Photo.serializer(), photo) shouldBe ENCODED
            Json.decodeFromString(Photo.serializer(), ENCODED) shouldBe photo
        }

        test("are refused when they are no Base64") {
            val failure = shouldThrow<SerializationException> {
                Json.decodeFromString(Photo.serializer(), """{"thumbnail":"*","file":"aGk="}""")
            }

            failure.message shouldContain "is no Base64"
        }
    }
})
