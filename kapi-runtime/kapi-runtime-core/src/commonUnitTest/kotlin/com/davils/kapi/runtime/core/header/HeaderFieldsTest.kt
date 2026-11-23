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
import com.davils.kapi.runtime.core.fieldLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength

private const val RETRY_DATE: String = "Fri, 31 Dec 1999 23:59:59 GMT"

private const val RETRY_SECONDS: Long = 946_684_799L

private const val TWO_MINUTES: Long = 120L

private const val KEY: String = "8e03978e-40d5-43e8-bc93-6894a57f9324"

private const val LONGEST_KEY: Int = 255

private const val TOO_MANY_DIGITS: Int = 19

private const val INLINE: String = "inline"

private const val ATTACHMENT: String = "attachment"

private fun quoted(text: String): String = "\"$text\""

private const val EURO_FILE: String = "€ rates"

private const val NEXT_PAGE: String = "https://api.example.com/pets?page=2"

private fun links(text: String): List<LinkValue> = Links.parseOrNull(text).shouldNotBeNull()

private fun disposition(text: String): ContentDisposition = ContentDisposition.parseOrNull(text).shouldNotBeNull()

class HeaderFieldsTest : FunSpec({
    context("Retry-After") {
        test("reads the examples of RFC 9110 §10.2.3") {
            RetryAfter.parseOrNull(RETRY_DATE) shouldBe RetryAfter.Date(HttpDate(RETRY_SECONDS))
            RetryAfter.parseOrNull("120") shouldBe RetryAfter.Delay(TWO_MINUTES)
            RetryAfter.parseOrNull(" 120 ") shouldBe RetryAfter.Delay(TWO_MINUTES)
            RetryAfter.Date(HttpDate(RETRY_SECONDS)).toString() shouldBe RETRY_DATE
        }

        test("counts the seconds to wait and never less than zero") {
            val now = HttpDate(RETRY_SECONDS - TWO_MINUTES)

            RetryAfter.Delay(TWO_MINUTES).secondsFrom(now) shouldBe TWO_MINUTES
            RetryAfter.Date(HttpDate(RETRY_SECONDS)).secondsFrom(now) shouldBe TWO_MINUTES
            RetryAfter.Date(HttpDate(0L)).secondsFrom(now) shouldBe 0L
        }

        test("refuses what is neither a delay nor a date") {
            listOf("", "-1", "1.5", "soon", "1".repeat(TOO_MANY_DIGITS), "120\r\nX: y").forEach { text ->
                RetryAfter.parseOrNull(text).shouldBeNull()
            }
            shouldThrow<IllegalArgumentException> { RetryAfter.Delay(-1L) }
            RetryAfter.Delay(1L).hashCode() shouldBe RetryAfter.Delay(1L).hashCode()
            RetryAfter.Date(HttpDate(0L)).hashCode() shouldBe HttpDate(0L).hashCode()
        }
    }

    context("Link") {
        test("reads the examples of RFC 8288 §3.5") {
            val chapter = links("""<http://example.com/TheBook/chapter2>; rel="previous"; title="previous chapter"""")
            val euro = links(
                """</TheBook/chapter2>; rel="previous"; title*=UTF-8'de'letztes%20Kapitel, """ +
                    """</TheBook/chapter4>; rel="next"; title*=UTF-8'de'n%c3%a4chstes%20Kapitel""",
            )
            val multiple = links("""<http://example.org/>; rel="start http://example.net/relation/other"""")

            chapter.single().relations shouldBe listOf("previous")
            chapter.single().parameter("TITLE") shouldBe "previous chapter"
            euro.map { link -> link.parameter("title*") } shouldBe listOf("letztes Kapitel", "nächstes Kapitel")
            multiple.single().relations shouldBe listOf("start", "http://example.net/relation/other")
        }

        test("finds the pages of a paged result") {
            val page = links("<$NEXT_PAGE>; rel=next, <https://api.example.com/pets?page=9>; rel=\"last\"")

            Links.find(page, Links.NEXT).shouldNotBeNull().target shouldBe NEXT_PAGE
            Links.find(page, "LAST").shouldNotBeNull().target shouldBe "https://api.example.com/pets?page=9"
            Links.find(page, Links.PREV).shouldBeNull()
            listOf(Links.FIRST, Links.LAST) shouldBe listOf("first", "last")
        }

        test("allows blanks around the assignment and parameters without a value and keeps the first of a name") {
            val link = links("<a> ; rel = next ; crossorigin ; rel=last").single()

            link.relations shouldBe listOf("next")
            link.parameter("crossorigin") shouldBe ""
        }

        test("writes links back, with Unicode titles as ext-values") {
            val link = LinkValue(NEXT_PAGE, listOf("rel" to "next", "title*" to "nächste Seite"))

            Links.format(listOf(link, LinkValue("/a"))) shouldBe
                "<$NEXT_PAGE>; rel=\"next\"; title*=UTF-8''n%C3%A4chste%20Seite, </a>"
            links(Links.format(listOf(link))) shouldBe listOf(link)
        }

        test("refuses malformed links, injections and values above the limits") {
            val refused = listOf(
                "a; rel=next",
                "<a",
                "<a b>",
                "<a>; rel=\"open",
                "<a>; =x",
                "<a>; title*=UTF-16''x",
                "<a>; rel=next\r\nSet-Cookie: x",
                "<a>junk",
            )
            refused.forEach { text -> Links.parseOrNull(text).shouldBeNull() }
            Links.parseOrNull("<a>, <b>", fieldLimits { maxMembers = 1 }).shouldBeNull()
            Links.parseOrNull("<a>; a; b", fieldLimits { maxParameters = 1 }).shouldBeNull()
            shouldThrow<IllegalArgumentException> { LinkValue("a b") }
            shouldThrow<IllegalArgumentException> { LinkValue("a>") }
            shouldThrow<IllegalArgumentException> { LinkValue("/a", listOf("rel" to "next\r\nX: y")) }
            shouldThrow<IllegalArgumentException> { LinkValue("/a", listOf("rel" to "a", "REL" to "b")) }
        }

        test("is equal by its target and parameters") {
            LinkValue("/a") shouldBe LinkValue("/a")
            LinkValue("/a").hashCode() shouldBe LinkValue("/a").hashCode()
            LinkValue("/a") shouldNotBe LinkValue("/b")
        }
    }

    context("Content-Disposition") {
        test("reads the examples of RFC 6266 §5") {
            disposition("Attachment; filename=example.html").filename shouldBe "example.html"
            disposition("""INLINE; FILENAME= "an example.html"""").type shouldBe INLINE
            disposition("attachment; filename*= UTF-8''%e2%82%ac%20rates").filename shouldBe EURO_FILE
            disposition("""attachment; filename="EURO rates"; filename*=utf-8''%e2%82%ac%20rates""").filename shouldBe
                EURO_FILE
        }

        test("reads form-data names and Unicode in quoted strings and finds a safe file name") {
            val field = disposition("""form-data; name="avatar"; filename="../../etc/pässwd"""")

            field.name shouldBe "avatar"
            field.filename shouldBe "../../etc/pässwd"
            field.safeFilename shouldBe "pässwd"
            disposition("""attachment; filename=".."""").safeFilename.shouldBeNull()
            disposition(INLINE).filename.shouldBeNull()
        }

        test("writes a fallback and filename* for a name that is not ASCII") {
            ContentDisposition.attachment(EURO_FILE).toString() shouldBe
                """attachment; filename="_ rates"; filename*=UTF-8''%E2%82%AC%20rates"""
            ContentDisposition.inline("""a "b".txt""").toString() shouldBe """inline; filename="a \"b\".txt""""
            ContentDisposition.attachment().toString() shouldBe ATTACHMENT
            disposition(ContentDisposition.attachment(EURO_FILE).toString()).filename shouldBe EURO_FILE
        }

        test("refuses what would inject a field and what is malformed") {
            shouldThrow<IllegalArgumentException> { ContentDisposition.attachment("a\r\nSet-Cookie: x") }
            shouldThrow<IllegalArgumentException> { ContentDisposition(ATTACHMENT, listOf("x" to "é")) }
            shouldThrow<IllegalArgumentException> { ContentDisposition("a b") }
            val refused = listOf(
                "", "attachment; filename=a; filename=b", "attachment; filename=\"a\rb\"", "attachment; filename*=x",
                "attachment; filename*=UTF-16''a", "attachment;", "attachment; filename", "attachment junk",
                "attachment; filename=\"open", ATTACHMENT + "; a=b".repeat(33), "a".repeat(8_193),
            )
            refused.forEach { text -> ContentDisposition.parseOrNull(text).shouldBeNull() }
        }

        test("is equal by its type and parameters") {
            val inline = disposition(INLINE)

            inline shouldBe ContentDisposition.inline()
            inline.hashCode() shouldBe ContentDisposition("INLINE").hashCode()
            inline.parameter("x").shouldBeNull()
        }
    }

    context("Idempotency-Key") {
        test("reads and writes the String of the draft") {
            val key = IdempotencyKey.parseOrNull(quoted(KEY)).shouldNotBeNull()

            key.value shouldBe KEY
            key.toString() shouldBe quoted(KEY)
            IdempotencyKey.parseOrNull(quoted(KEY) + ";scope=1") shouldBe key
        }

        test("makes random keys of a UUID") {
            IdempotencyKey.random().value shouldHaveLength KEY.length
            IdempotencyKey.random() shouldNotBe IdempotencyKey.random()
        }

        test("refuses what is no String, is empty or is too long") {
            listOf(KEY, quoted(""), "1", """"a", "b"""", quoted("a".repeat(LONGEST_KEY + 1))).forEach { text ->
                IdempotencyKey.parseOrNull(text).shouldBeNull()
            }
            IdempotencyKey.parseOrNull(quoted("a".repeat(LONGEST_KEY))).shouldNotBeNull()
            shouldThrow<IllegalArgumentException> { IdempotencyKey("a\r\nb") }
            shouldThrow<IllegalArgumentException> { IdempotencyKey("") }
        }
    }
})
