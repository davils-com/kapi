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

package com.davils.kapi.runtime.core.header

import com.davils.kapi.runtime.core.conditional.HttpDate
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val ATTACHMENT: String = "attachment"

private const val FILENAME: String = "filename"

private const val EXTENDED_FILENAME: String = "filename*"

private const val TARGET: String = "/a"

private const val TITLE: String = "title"

private const val LONGEST_KEY: Int = 255

private const val DELAY: Long = 5L

private val file: Pair<String, String> = FILENAME to "a.txt"

private fun links(text: String): List<LinkValue>? = Links.parseOrNull(text)

class HeaderValuesTest : FunSpec({
    context("Content-Disposition") {
        test("refuses a parameter named twice") {
            shouldThrow<IllegalArgumentException> { ContentDisposition(ATTACHMENT, listOf(file, file)) }
            shouldThrow<IllegalArgumentException> { ContentDisposition(ATTACHMENT, listOf("a b" to "x")) }
        }

        test("has no safe file name without a file name") {
            ContentDisposition(ATTACHMENT).safeFilename.shouldBeNull()
            ContentDisposition(ATTACHMENT, listOf(FILENAME to "dir\\ ")).safeFilename.shouldBeNull()
        }

        test("differs in its type and its parameters") {
            val disposition = ContentDisposition(ATTACHMENT, listOf(file))

            disposition shouldNotBe ContentDisposition("inline", listOf(file))
            disposition shouldNotBe ContentDisposition(ATTACHMENT)
            disposition.equals(ATTACHMENT) shouldBe false
        }

        test("writes filename* as an extended value and no second one for a name with it") {
            val both = ContentDisposition(ATTACHMENT, listOf(FILENAME to "é", EXTENDED_FILENAME to "é"))

            both.toString() shouldBe "attachment; filename=\"_\"; filename*=UTF-8''%C3%A9"
            both.filename shouldBe "é"
        }

        test("refuses a parameter without a name") {
            ContentDisposition.parseOrNull("attachment; =a").shouldBeNull()
            ContentDisposition.parseOrNull("attachment; filename*=UTF-8''%zz").shouldBeNull()
        }
    }

    context("Link") {
        test("refuses a target that is missing, unclosed or no URI reference") {
            links("<>").shouldBeNull()
            links("</a").shouldBeNull()
            links("/a>").shouldBeNull()
            links("<a b>").shouldBeNull()
            links("<%zz>").shouldBeNull()
        }

        test("refuses a parameter without a name and a value a field cannot carry") {
            links("<$TARGET>; =x").shouldBeNull()
            links("<$TARGET>; title=\"é\"").shouldBeNull()
            links("<$TARGET>; title*=UTF-8''%zz").shouldBeNull()
        }

        test("keeps the first of a repeated parameter and reads one without a value") {
            val link = links("<$TARGET>; rel=next; rel=prev; hidden").shouldNotBeNull().single()

            link.relations shouldBe listOf("next")
            link.parameter("hidden") shouldBe ""
        }

        test("reads an extended title") {
            val link = links("<$TARGET>; title*=UTF-8''%C3%A9").shouldNotBeNull().single()

            link.parameter("TITLE*") shouldBe "é"
            link.toString() shouldBe "<$TARGET>; title*=UTF-8''%C3%A9"
        }
    }

    context("a link value") {
        test("refuses parameters that are no tokens or are named twice") {
            shouldThrow<IllegalArgumentException> { LinkValue(TARGET, listOf("a b" to "x")) }
            shouldThrow<IllegalArgumentException> { LinkValue(TARGET, listOf(TITLE to "x", TITLE to "y")) }
            shouldThrow<IllegalArgumentException> { LinkValue(TARGET, listOf(TITLE to "\r\n")) }
            shouldThrow<IllegalArgumentException> { LinkValue("<%zz>") }
        }

        test("has no relations and no parameters it does not carry") {
            val link = LinkValue(TARGET)

            link.relations shouldBe emptyList()
            link.parameter(TITLE).shouldBeNull()
        }

        test("splits its relations at spaces and tabs") {
            LinkValue(TARGET, listOf("rel" to "Next\t prev")).relations shouldBe listOf("next", "prev")
        }

        test("differs in its target and its parameters") {
            val link = LinkValue(TARGET, listOf(TITLE to "x"))

            link shouldNotBe LinkValue(TARGET, listOf(TITLE to "y"))
            link shouldNotBe LinkValue("/b", listOf(TITLE to "x"))
            link.equals(TARGET) shouldBe false
        }
    }

    context("Retry-After") {
        test("differs by kind and by value") {
            val delay = RetryAfter.Delay(DELAY)
            val date = RetryAfter.Date(HttpDate(DELAY))

            delay.toString() shouldBe "5"
            delay.equals(date) shouldBe false
            date.equals(delay) shouldBe false
            date shouldNotBe RetryAfter.Date(HttpDate(0L))
            shouldThrow<IllegalArgumentException> { RetryAfter.Delay(-1L) }
        }
    }

    context("Idempotency-Key") {
        test("refuses a key that is too long or not printable") {
            shouldThrow<IllegalArgumentException> { IdempotencyKey("a".repeat(LONGEST_KEY + 1)) }
            shouldThrow<IllegalArgumentException> { IdempotencyKey("é") }
            IdempotencyKey("a".repeat(LONGEST_KEY)).value.length shouldBe LONGEST_KEY
        }
    }
})
