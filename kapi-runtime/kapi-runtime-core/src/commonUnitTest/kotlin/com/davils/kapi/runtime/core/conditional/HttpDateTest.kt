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

package com.davils.kapi.runtime.core.conditional

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val RFC_EXAMPLE: String = "Sun, 06 Nov 1994 08:49:37 GMT"

private const val RFC_SECONDS: Long = 784_111_777L

private const val RFC_YEAR: Int = 1_994

private const val NOW_SECONDS: Long = 1_795_000_000L

private const val YEAR_ONE: Long = -62_135_596_800L

private const val LAST: Long = 253_402_300_799L

private const val LEAP_DAY: Long = 951_782_400L

private val now: HttpDate = HttpDate(NOW_SECONDS)

private val refused: List<String> = listOf(
    "",
    "Mon, 06 Nov 1994 08:49:37 GMT",
    "Sun, 6 Nov 1994 08:49:37 GMT",
    "Sun, 06 Nov 1994 08:49:37 UTC",
    "Sun, 06 Xyz 1994 08:49:37 GMT",
    "Sun, 31 Nov 1994 08:49:37 GMT",
    "Sun, 06 Nov 1994 24:00:00 GMT",
    "Sun, 06 Nov 1994 08:60:00 GMT",
    "Sun, 06 Nov 1994 08:49:61 GMT",
    "Sun, 06 Nov 94 08:49:37 GMT",
    "Sun, 06 Nov 1994 8:49:37 GMT",
    "Sun,  06 Nov 1994 08:49:37 GMT",
    "Sun, 06 Nov 1994 08:49:37 GMT\r\n",
    "Tue, 29 Feb 1900 00:00:00 GMT",
    "Sun Nov 06 08:49:37 1994 GMT",
    "Sun Nov 6 08:49:37 1994",
    "Sun, 06 Nov 1994 08:49:37 GMT, Sun, 06 Nov 1994 08:49:37 GMT",
    "x".repeat(41),
)

class HttpDateTest : FunSpec({
    context("reading") {
        test("reads the IMF-fixdate of RFC 9110 and writes it back") {
            val date = HttpDate.parse(RFC_EXAMPLE)

            date.epochSeconds shouldBe RFC_SECONDS
            date.toString() shouldBe RFC_EXAMPLE
            date.year shouldBe RFC_YEAR
        }

        test("reads the obsolete asctime format") {
            HttpDate.parse("Sun Nov  6 08:49:37 1994").epochSeconds shouldBe RFC_SECONDS
        }

        test("reads the obsolete RFC 850 format only relative to the present") {
            HttpDate.parse("Sunday, 06-Nov-94 08:49:37 GMT", now).epochSeconds shouldBe RFC_SECONDS
            HttpDate.parseOrNull("Sunday, 06-Nov-94 08:49:37 GMT").shouldBeNull()
            HttpDate.parse("Tuesday, 29-Feb-00 00:00:00 GMT", now).epochSeconds shouldBe LEAP_DAY
        }

        refused.forEach { text ->
            test("refuses '$text'") {
                HttpDate.parseOrNull(text, now).shouldBeNull()
            }
        }

        test("throws for what is no date") {
            shouldThrow<IllegalArgumentException> { HttpDate.parse("yesterday") }
        }
    }

    context("a date") {
        test("lies between the years 1 and 9999 and is written with four digits") {
            HttpDate(YEAR_ONE).toString() shouldBe "Mon, 01 Jan 0001 00:00:00 GMT"
            HttpDate(LAST).toString() shouldBe "Fri, 31 Dec 9999 23:59:59 GMT"
            HttpDate(0L).toString() shouldBe "Thu, 01 Jan 1970 00:00:00 GMT"
            shouldThrow<IllegalArgumentException> { HttpDate(YEAR_ONE - 1) }
            shouldThrow<IllegalArgumentException> { HttpDate(LAST + 1) }
        }

        test("orders by time") {
            (HttpDate(0L) < now) shouldBe true
            HttpDate.parse(HttpDate(LEAP_DAY).toString()) shouldBe HttpDate(LEAP_DAY)
        }
    }
})
