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

package com.davils.kapi.runtime.core.response

import com.davils.kapi.runtime.core.response.fixture.Answer
import com.davils.kapi.runtime.core.response.fixture.COUNT
import com.davils.kapi.runtime.core.response.fixture.single
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val TOKEN: String = "Bearer abc.def"

private const val TRACE: String = "X-Trace"

private const val TRACE_LOWER: String = "x-trace"

private const val AUTHORIZATION: String = "authorization"

private const val SESSION: String = "x-session"

private const val MISSING: String = "missing"

private const val INFORMATION: Int = 199

private const val OK: Int = 200

private const val LAST_SUCCESS: Int = 299

private const val REDIRECT: Int = 300

private const val LIMIT: Int = 42

private const val TOTAL: Long = 9_000_000_000

private const val RATIO: Double = 0.5

private val headers: Headers = headersOf(
    entries = mapOf(
        "Authorization" to listOf(TOKEN),
        TRACE to listOf("a"),
        TRACE_LOWER to listOf("b"),
        "X-Session" to listOf("s1"),
    ),
    sensitive = setOf("X-Session"),
)

@OptIn(DavilsInternal::class)
class HeadersTest : FunSpec({
    context("reading") {
        test("finds a field whatever the case of its name") {
            headers[AUTHORIZATION] shouldBe TOKEN
            headers["AUTHORIZATION"] shouldBe TOKEN
            headers[MISSING] shouldBe null
            (SESSION in headers) shouldBe true
            (MISSING in headers) shouldBe false
        }

        test("merges the values of names that differ in case only") {
            headers.all(TRACE) shouldBe listOf("a", "b")
            headers.all(MISSING) shouldBe emptyList()
            headers.names shouldBe setOf(AUTHORIZATION, TRACE_LOWER, SESSION)
            headers.toMap()[AUTHORIZATION] shouldBe listOf(TOKEN)
        }
    }

    context("masking") {
        test("hides the fields that carry credentials and the ones named sensitive") {
            val masked = headers.masked()

            masked[AUTHORIZATION] shouldBe listOf(SafeText.REDACTED)
            masked[SESSION] shouldBe listOf(SafeText.REDACTED)
            masked[TRACE_LOWER] shouldBe listOf("a", "b")
        }

        test("never renders a hidden value and escapes every other") {
            val rendered = headersOf(mapOf("X-Note" to listOf("a\r\nb"), "Cookie" to listOf("id=1"))).toString()

            headers.toString() shouldNotContain "abc.def"
            headers.toString() shouldNotContain "s1"
            headers.toString() shouldContain TRACE_LOWER
            rendered shouldNotContain "\r\n"
            rendered shouldNotContain "id=1"
        }
    }

    context("equality") {
        test("compares names without case and values in order") {
            val same = headersOf(mapOf(TRACE_LOWER to listOf("a")))

            same shouldBe headersOf(mapOf(TRACE to listOf("a")))
            same.hashCode() shouldBe headersOf(mapOf(TRACE to listOf("a"))).hashCode()
            same shouldNotBe headersOf(mapOf(TRACE to listOf("b")))
            same.equals(TRACE) shouldBe false
            Headers.Empty.names shouldBe emptySet()
        }
    }

    context("a response") {
        test("is a success from 200 to 299") {
            Answer(INFORMATION).isSuccess shouldBe false
            Answer(OK).isSuccess shouldBe true
            Answer(LAST_SUCCESS).isSuccess shouldBe true
            Answer(REDIRECT).isSuccess shouldBe false
            Answer(OK).headers shouldBe Headers.Empty
        }
    }

    context("a field that is a number or a flag") {
        test("is read as the kind the caller asks for, whatever space surrounds it") {
            single(" 42 ").int(COUNT) shouldBe LIMIT
            single("9000000000").long(COUNT) shouldBe TOTAL
            single("0.5").double(COUNT) shouldBe RATIO
            single("0.5").float(COUNT) shouldBe RATIO.toFloat()
            single("true").flag(COUNT) shouldBe true
            single("false").flag(COUNT) shouldBe false
        }

        test("is nothing when it is no such value or absent") {
            single("many").int(COUNT).shouldBeNull()
            single("9000000000").int(COUNT).shouldBeNull()
            single("4.2").long(COUNT).shouldBeNull()
            single("half").double(COUNT).shouldBeNull()
            single("half").float(COUNT).shouldBeNull()
            single("yes").flag(COUNT).shouldBeNull()
            Headers.Empty.int(COUNT).shouldBeNull()
            Headers.Empty.long(COUNT).shouldBeNull()
            Headers.Empty.double(COUNT).shouldBeNull()
            Headers.Empty.float(COUNT).shouldBeNull()
            Headers.Empty.flag(COUNT).shouldBeNull()
        }
    }
})
