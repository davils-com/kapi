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

import com.davils.kapi.runtime.core.fieldLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val GET: String = "GET"

private const val PUT: String = "PUT"

private const val TAG: String = "\"xyzzy\""

private const val OLDER: String = "Sat, 05 Nov 1994 08:49:37 GMT"

private const val NEWER: String = "Mon, 07 Nov 1994 08:49:37 GMT"

private const val MODIFIED: Long = 784_111_777L

private const val LONGEST_TAG: Int = 8_192

private const val CRLF: String = "\r\n"

private const val WEAK_TAG: String = "W/$TAG"

private const val LIST: String = """"a", W/"b""""

private fun quoted(text: String): String = "\"$text\""

private fun ifRange(text: String): IfRange = IfRange.parseOrNull(text).shouldNotBeNull()

private val current: EntityTag = EntityTag("xyzzy")

private val state: RepresentationState = RepresentationState(current, HttpDate(MODIFIED))

private fun decide(method: String = GET, current: RepresentationState = state, read: () -> Preconditions) =
    read().evaluate(method, current)

class PreconditionsTest : FunSpec({
    context("an entity tag") {
        test("compares as the table of RFC 9110 §8.8.3.2 says") {
            val weakOne = EntityTag.parse("W/\"1\"")
            val weakTwo = EntityTag.parse("W/\"2\"")
            val strongOne = EntityTag.parse("\"1\"")

            weakOne.matchesStrongly(weakOne) shouldBe false
            weakOne.matchesWeakly(weakOne) shouldBe true
            weakOne.matchesStrongly(weakTwo) shouldBe false
            weakOne.matchesWeakly(weakTwo) shouldBe false
            weakOne.matchesStrongly(strongOne) shouldBe false
            weakOne.matchesWeakly(strongOne) shouldBe true
            strongOne.matchesStrongly(strongOne) shouldBe true
        }

        test("is read and written with its quotes") {
            EntityTag.parse(" W/\"a-b\" ").toString() shouldBe "W/\"a-b\""
            EntityTag.parse("\"\"") shouldBe EntityTag("")
            EntityTag("a").hashCode() shouldBe EntityTag("a").hashCode()
            EntityTag("a") shouldNotBe EntityTag("a", isWeak = true)
            EntityTag("a").equals("a") shouldBe false
        }

        test("refuses what is no entity tag and what a field cannot carry") {
            listOf(
                "xyzzy",
                """"open""",
                """w/"a"""",
                """"a"b""",
                """"a b"""",
                """"é"""",
                quoted("a") + CRLF,
            ).forEach { text ->
                EntityTag.parseOrNull(text).shouldBeNull()
            }
            EntityTag.parseOrNull(quoted("a".repeat(LONGEST_TAG))).shouldBeNull()
            shouldThrow<IllegalArgumentException> { EntityTag("a\"b") }
            shouldThrow<IllegalArgumentException> { EntityTag("a\r\nb") }
            shouldThrow<IllegalArgumentException> { EntityTag.parse("x") }
        }
    }

    context("a list of entity tags") {
        test("is read with any and with tags") {
            EntityTagCondition.parseOrNull(" * ") shouldBe EntityTagCondition.Any
            EntityTagCondition.parseOrNull("$LIST,,") shouldBe
                EntityTagCondition.Tags(listOf(EntityTag("a"), EntityTag("b", isWeak = true)))
            val listed = EntityTagCondition.parseOrNull(LIST).shouldNotBeNull()

            listed.toString() shouldBe LIST
            EntityTagCondition.Any.toString() shouldBe "*"
        }

        test("refuses a malformed list and one above the limit") {
            val limits = fieldLimits { maxMembers = 2 }

            EntityTagCondition.parseOrNull("\"a\", *").shouldBeNull()
            EntityTagCondition.parseOrNull("a").shouldBeNull()
            EntityTagCondition.parseOrNull(""""a", "b"""", limits).shouldNotBeNull()
            EntityTagCondition.parseOrNull(""""a", "b", "c"""", limits).shouldBeNull()
        }
    }

    context("evaluate") {
        test("proceeds without preconditions") {
            decide { Preconditions() } shouldBe PreconditionDecision.Proceed
        }

        test("fails If-Match that does not match strongly") {
            decide(PUT) { Preconditions.read(ifMatch = TAG) } shouldBe PreconditionDecision.Proceed
            decide(PUT) { Preconditions.read(ifMatch = WEAK_TAG) } shouldBe PreconditionDecision.PreconditionFailed
            decide(PUT) { Preconditions.read(ifMatch = "*") } shouldBe PreconditionDecision.Proceed
            decide(PUT, RepresentationState(isPresent = false)) { Preconditions.read(ifMatch = "*") } shouldBe
                PreconditionDecision.PreconditionFailed
            decide(PUT, RepresentationState()) { Preconditions.read(ifMatch = TAG) } shouldBe
                PreconditionDecision.PreconditionFailed
        }

        test("fails a malformed If-Match and passes a malformed If-None-Match") {
            decide(PUT) { Preconditions.read(ifMatch = "garbage") } shouldBe PreconditionDecision.PreconditionFailed
            decide { Preconditions.read(ifNoneMatch = "garbage") } shouldBe PreconditionDecision.Proceed
        }

        test("answers a matching If-None-Match with 304 for reads and 412 for writes") {
            decide { Preconditions.read(ifNoneMatch = WEAK_TAG) } shouldBe PreconditionDecision.NotModified
            decide(PUT) { Preconditions.read(ifNoneMatch = "*") } shouldBe PreconditionDecision.PreconditionFailed
            decide(PUT, RepresentationState(isPresent = false)) { Preconditions.read(ifNoneMatch = "*") } shouldBe
                PreconditionDecision.Proceed
            decide { Preconditions.read(ifNoneMatch = "\"other\"") } shouldBe PreconditionDecision.Proceed
        }

        test("evaluates If-Unmodified-Since only without If-Match") {
            decide(
                PUT,
            ) { Preconditions.read(ifUnmodifiedSince = OLDER) } shouldBe PreconditionDecision.PreconditionFailed
            decide(PUT) { Preconditions.read(ifUnmodifiedSince = NEWER) } shouldBe PreconditionDecision.Proceed
            decide(PUT) { Preconditions.read(ifMatch = TAG, ifUnmodifiedSince = OLDER) } shouldBe
                PreconditionDecision.Proceed
        }

        test("evaluates If-Modified-Since only for reads and only without If-None-Match") {
            decide { Preconditions.read(ifModifiedSince = NEWER) } shouldBe PreconditionDecision.NotModified
            decide { Preconditions.read(ifModifiedSince = OLDER) } shouldBe PreconditionDecision.Proceed
            decide(PUT) { Preconditions.read(ifModifiedSince = NEWER) } shouldBe PreconditionDecision.Proceed
            decide { Preconditions.read(ifNoneMatch = "\"other\"", ifModifiedSince = NEWER) } shouldBe
                PreconditionDecision.Proceed
            decide { Preconditions.read(ifModifiedSince = "not a date") } shouldBe PreconditionDecision.Proceed
            decide(current = RepresentationState(current)) { Preconditions.read(ifModifiedSince = NEWER) } shouldBe
                PreconditionDecision.Proceed
        }

        test("is equal by its conditions") {
            Preconditions.read(ifMatch = TAG) shouldBe Preconditions(ifMatch = EntityTagCondition.Tags(listOf(current)))
            Preconditions().hashCode() shouldBe Preconditions().hashCode()
            Preconditions().toString() shouldBe
                "Preconditions(ifMatch=none, ifNoneMatch=none, ifModifiedSince=none, ifUnmodifiedSince=none)"
            state shouldBe RepresentationState(current, HttpDate(MODIFIED))
            state.hashCode() shouldBe RepresentationState(current, HttpDate(MODIFIED)).hashCode()
            state.toString() shouldBe
                "RepresentationState(entityTag=\"xyzzy\", lastModified=Sun, 06 Nov 1994 08:49:37 GMT, isPresent=true)"
        }
    }

    context("If-Range") {
        test("matches a strong tag and an exact date only") {
            ifRange(TAG).matches(state) shouldBe true
            ifRange(WEAK_TAG).matches(state) shouldBe false
            ifRange(quoted("old")).matches(state) shouldBe false
            ifRange(TAG).matches(RepresentationState()) shouldBe false
            ifRange(HttpDate(MODIFIED).toString()).matches(state) shouldBe true
            ifRange(NEWER).matches(state) shouldBe false
            IfRange.parseOrNull("tomorrow").shouldBeNull()
        }

        test("is equal by its value") {
            IfRange.Tag(current) shouldBe IfRange.Tag(current)
            IfRange.Tag(current).hashCode() shouldBe current.hashCode()
            IfRange.Tag(current).toString() shouldBe TAG
            IfRange.Date(HttpDate(MODIFIED)) shouldBe IfRange.Date(HttpDate(MODIFIED))
            IfRange.Date(HttpDate(MODIFIED)).hashCode() shouldBe HttpDate(MODIFIED).hashCode()
        }
    }
})
