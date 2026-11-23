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

package com.davils.kapi.runtime.core.cache

import com.davils.kapi.runtime.core.fieldLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val HOUR: Long = 3_600L

private const val MINUTE: Long = 60L

private const val DAY: Long = 86_400L

private const val CEILING: Long = 2_147_483_648L

private const val SET_COOKIE: String = "set-cookie"

private const val TEN: Long = 10L

private const val FIVE: Long = 5L

private fun quoted(text: String): String = "\"$text\""

private fun control(text: String): CacheControl = CacheControl.parseOrNull(text).shouldNotBeNull()

class CacheControlTest : FunSpec({
    context("a response") {
        test("reads every directive of RFC 9111 §5.2.2 and RFC 5861 and RFC 8246") {
            val control = control(
                "public, max-age=3600, s-maxage=60, must-revalidate, proxy-revalidate, no-transform, " +
                    "must-understand, immutable, stale-while-revalidate=60, stale-if-error=86400",
            )

            control.isPublic shouldBe true
            control.maxAge shouldBe HOUR
            control.sharedMaxAge shouldBe MINUTE
            control.isMustRevalidate shouldBe true
            control.isProxyRevalidate shouldBe true
            control.isNoTransform shouldBe true
            control.isMustUnderstand shouldBe true
            control.isImmutable shouldBe true
            control.staleWhileRevalidate shouldBe MINUTE
            control.staleIfError shouldBe DAY
            control.isNoStore shouldBe false
            control.isPrivate shouldBe false
        }

        test("reads the field names of no-cache and private") {
            val control = control("no-cache=\"Set-Cookie, Authorization\", private=Set-Cookie")

            control.isNoCache shouldBe true
            control.noCacheFields shouldBe listOf(SET_COOKIE, "authorization")
            control.privateFields shouldBe listOf(SET_COOKIE)
            control("no-cache").noCacheFields shouldBe emptyList()
        }
    }

    context("a request") {
        test("reads every directive of RFC 9111 §5.2.1") {
            val control = control("max-age=0, max-stale, min-fresh=60, no-store, only-if-cached")

            control.maxAge shouldBe 0L
            control.isMaxStale shouldBe true
            control.maxStale.shouldBeNull()
            control.minFresh shouldBe MINUTE
            control.isNoStore shouldBe true
            control.isOnlyIfCached shouldBe true
            control("max-stale=60").maxStale shouldBe MINUTE
        }
    }

    context("reading") {
        test("takes the first of repeated directives and names without case") {
            control("Max-Age=10, max-age=20").maxAge shouldBe TEN
            ("MAX-AGE" in control("max-age=1")) shouldBe true
        }

        test("caps a delta above 2^31 and ignores a malformed one") {
            control("max-age=99999999999999999999999").maxAge shouldBe CEILING
            control("max-age=2147483647").maxAge shouldBe CEILING - 1
            control("max-age=0000000000000005").maxAge shouldBe FIVE
            control("max-age=\"-1\"").maxAge.shouldBeNull()
            control("max-age=1.5").maxAge.shouldBeNull()
            CacheControl.parseOrNull("max-age=").shouldBeNull()
        }

        test("keeps unknown directives and writes the field back") {
            val text = "community=\"UCI\", max-age=60, x-flag"

            control(text).toString() shouldBe "community=UCI, max-age=60, x-flag"
        }

        test("refuses malformed values and values above the limits") {
            listOf("max-age=1=2", "max age", "=1", """a="open""", "a=" + quoted("x\ny"), "a; b").forEach { text ->
                CacheControl.parseOrNull(text).shouldBeNull()
            }
            CacheControl.parseOrNull("a, b", fieldLimits { maxMembers = 2 }).shouldNotBeNull()
            CacheControl.parseOrNull("a, b, c", fieldLimits { maxMembers = 2 }).shouldBeNull()
        }
    }

    context("writing") {
        test("writes the directives of the companion") {
            val control = CacheControl.of(
                CacheDirective.maxAge(HOUR),
                CacheDirective.sharedMaxAge(MINUTE),
                CacheDirective.maxStale(),
                CacheDirective.maxStale(1L),
                CacheDirective.minFresh(1L),
                CacheDirective.staleWhileRevalidate(1L),
                CacheDirective.staleIfError(1L),
                CacheDirective.noCache(listOf("Set-Cookie", "ETag")),
                CacheDirective.private(),
                CacheDirective.flag("no-store"),
            )

            control.toString() shouldBe "max-age=3600, s-maxage=60, max-stale, max-stale=1, min-fresh=1, " +
                "stale-while-revalidate=1, stale-if-error=1, no-cache=\"Set-Cookie, ETag\", private, no-store"
            CacheControl.parseOrNull(control.toString()) shouldBe control
        }

        test("refuses what would inject a line break or is no delta") {
            shouldThrow<IllegalArgumentException> {
                CacheDirective(
                    name = "max-age",
                    argument = "1\r\nSet-Cookie: a=b",
                )
            }
            shouldThrow<IllegalArgumentException> { CacheDirective("max age") }
            shouldThrow<IllegalArgumentException> { CacheDirective.maxAge(-1L) }
            shouldThrow<IllegalArgumentException> { CacheDirective.noCache(listOf("a b")) }
            shouldThrow<IllegalArgumentException> { Vary(listOf("a\r\nb")) }
        }

        test("compares directives and controls by value") {
            CacheDirective(name = "A", argument = "1") shouldBe CacheDirective(name = "a", argument = "1")
            CacheDirective("a").hashCode() shouldBe CacheDirective("A").hashCode()
            CacheDirective("a") shouldNotBe CacheDirective(name = "a", argument = "1")
            control("a").hashCode() shouldBe control("a").hashCode()
            control("a")["b"].shouldBeNull()
        }
    }

    context("Vary") {
        test("reads field names without case and the wildcard") {
            val vary = Vary.parseOrNull("Accept-Encoding, accept-language, Accept-Encoding").shouldNotBeNull()

            vary.fields shouldBe listOf("accept-encoding", "accept-language")
            ("ACCEPT-LANGUAGE" in vary) shouldBe true
            ("cookie" in vary) shouldBe false
            vary.toString() shouldBe "accept-encoding, accept-language"
            Vary.parseOrNull("*").shouldNotBeNull().isAny shouldBe true
            ("cookie" in Vary.parseOrNull("*, accept").shouldNotBeNull()) shouldBe true
            Vary(emptyList(), isAny = true).toString() shouldBe "*"
        }

        test("refuses a member that is no field name") {
            Vary.parseOrNull("accept language").shouldBeNull()
            Vary.parseOrNull("\"accept\"").shouldBeNull()
            Vary(listOf("a")) shouldBe Vary(listOf("A"))
            Vary(listOf("a")).hashCode() shouldBe Vary(listOf("A")).hashCode()
        }
    }
})
