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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val PRESENT: Long = 1_795_000_000L

private const val SECOND_BEFORE_EPOCH: Long = -1L

private const val DAY_BEFORE_EPOCH: Long = -86_400L

private const val YEAR_BEFORE_EPOCH: Int = 1_969

private const val MILLENNIUM_LEAP_DAY: Long = 951_782_400L

private const val SUNDAY_IN_1994: Long = 784_111_777L

private val present: HttpDate = HttpDate(PRESENT)

private val refusedRfc850: List<String> = listOf(
    "Sunday, 06-Nov-94 08:49:37 UTC",
    "Sunday, 06-Nov 08:49:37 GMT",
    "Sunday, 06-Nov-1994 08:49:37 GMT",
    "Sunday, 06-Nov-9x 08:49:37 GMT",
    "Sunday, 6-Nov-94 08:49:37 GMT",
    "Sunday 06-Nov-94 08:49:37 GMT",
    ", 06-Nov-94 08:49:37 GMT",
)

private val refusedAsctime: List<String> = listOf(
    "Sun Nov  06 08:49:37 1994",
    "Sun Nov 06 08:49:37 94",
    "Sun Nov 06 08:49:37 19x4",
    "Sun Nov  x 08:49:37 1994",
    "Sun Nov 06 08:49 1994",
)

private val refusedImf: List<String> = listOf(
    ", 06 Nov 1994 08:49:37 GMT",
    "Sat, 01 Jan 0000 00:00:00 GMT",
    "Sun, 00 Nov 1994 08:49:37 GMT",
    "Sun, 06 Nov 1994 0x:49:37 GMT",
    "Sun, 06 Nov 1994 08:4x:37 GMT",
    "Sun, 06 Nov 1994 08:49:3x GMT",
    "Sun, 06 Nov 1994 08:49:37:00 GMT",
    "Fri, 31 Dec 9999 23:59:60 GMT",
)

class HttpDateFormatsTest : FunSpec({
    context("the obsolete rfc850-date format") {
        refusedRfc850.forEach { text ->
            test("refuses the rfc850-date '$text'") {
                HttpDate.parseOrNull(text, present).shouldBeNull()
            }
        }
    }

    context("the obsolete asctime format") {
        test("reads a day of two digits") {
            HttpDate.parse("Sun Nov 06 08:49:37 1994").epochSeconds shouldBe SUNDAY_IN_1994
        }

        refusedAsctime.forEach { text ->
            test("refuses the asctime date '$text'") {
                HttpDate.parseOrNull(text).shouldBeNull()
            }
        }
    }

    context("the IMF-fixdate") {
        test("knows the leap day of a year divisible by 400") {
            HttpDate.parse("Tue, 29 Feb 2000 00:00:00 GMT").epochSeconds shouldBe MILLENNIUM_LEAP_DAY
        }

        refusedImf.forEach { text ->
            test("refuses the IMF-fixdate '$text'") {
                HttpDate.parseOrNull(text).shouldBeNull()
            }
        }
    }

    context("a date before 1970") {
        test("counts its days and years backwards") {
            HttpDate(SECOND_BEFORE_EPOCH).year shouldBe YEAR_BEFORE_EPOCH
            HttpDate(SECOND_BEFORE_EPOCH).toString() shouldBe "Wed, 31 Dec 1969 23:59:59 GMT"
            HttpDate(DAY_BEFORE_EPOCH).toString() shouldBe "Wed, 31 Dec 1969 00:00:00 GMT"
            HttpDate(DAY_BEFORE_EPOCH).year shouldBe YEAR_BEFORE_EPOCH
        }
    }
})
