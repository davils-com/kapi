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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val EARLIER: Long = 784_111_777L

private const val LATER: Long = 784_198_177L

private const val PUT: String = "PUT"

private val tag: EntityTag = EntityTag("v1")

private val otherTag: EntityTag = EntityTag("v2")

private val earlier: HttpDate = HttpDate(EARLIER)

private val later: HttpDate = HttpDate(LATER)

private val tags: EntityTagCondition = EntityTagCondition.Tags(listOf(tag))

private val otherTags: EntityTagCondition = EntityTagCondition.Tags(listOf(otherTag))

class ConditionalValuesTest : FunSpec({
    context("preconditions") {
        test("differ in each of their conditions") {
            val full = Preconditions(
                ifMatch = tags,
                ifNoneMatch = tags,
                ifModifiedSince = earlier,
                ifUnmodifiedSince = earlier,
            )

            full shouldBe Preconditions(
                ifMatch = tags,
                ifNoneMatch = tags,
                ifModifiedSince = earlier,
                ifUnmodifiedSince = earlier,
            )
            full shouldNotBe Preconditions(
                ifMatch = otherTags,
                ifNoneMatch = tags,
                ifModifiedSince = earlier,
                ifUnmodifiedSince = earlier,
            )
            full shouldNotBe Preconditions(
                ifMatch = tags,
                ifNoneMatch = otherTags,
                ifModifiedSince = earlier,
                ifUnmodifiedSince = earlier,
            )
            full shouldNotBe Preconditions(
                ifMatch = tags,
                ifNoneMatch = tags,
                ifModifiedSince = later,
                ifUnmodifiedSince = earlier,
            )
            full shouldNotBe Preconditions(
                ifMatch = tags,
                ifNoneMatch = tags,
                ifModifiedSince = earlier,
                ifUnmodifiedSince = later,
            )
            full.equals(tags) shouldBe false
        }

        test("show every condition they carry") {
            val full = Preconditions(
                ifMatch = EntityTagCondition.Any,
                ifNoneMatch = tags,
                ifModifiedSince = earlier,
                ifUnmodifiedSince = later,
            )

            full.toString() shouldBe "Preconditions(ifMatch=*, ifNoneMatch=\"v1\", " +
                "ifModifiedSince=Sun, 06 Nov 1994 08:49:37 GMT, ifUnmodifiedSince=Mon, 07 Nov 1994 08:49:37 GMT)"
        }

        test("pass If-Unmodified-Since for a representation without a date") {
            Preconditions(ifUnmodifiedSince = earlier).evaluate(PUT, RepresentationState()) shouldBe
                PreconditionDecision.Proceed
        }
    }

    context("a representation state") {
        test("differs in each of its parts") {
            val state = RepresentationState(tag, earlier)

            state shouldNotBe RepresentationState(otherTag, earlier)
            state shouldNotBe RepresentationState(tag, later)
            state shouldNotBe RepresentationState(tag, earlier, isPresent = false)
            state.equals(tag) shouldBe false
            RepresentationState().toString() shouldBe "RepresentationState(entityTag=none, lastModified=none, " +
                "isPresent=true)"
        }
    }

    context("an entity tag") {
        test("matches strongly only when neither side is weak") {
            tag.matchesStrongly(EntityTag("v1", isWeak = true)) shouldBe false
            tag shouldNotBe otherTag
        }
    }

    context("If-Range") {
        test("differs by kind and by value") {
            IfRange.Tag(tag).equals(IfRange.Date(earlier)) shouldBe false
            IfRange.Date(earlier).equals(IfRange.Tag(tag)) shouldBe false
            IfRange.Date(earlier) shouldNotBe IfRange.Date(later)
            IfRange.Date(earlier).toString() shouldBe "Sun, 06 Nov 1994 08:49:37 GMT"
            IfRange.Date(earlier).matches(RepresentationState()) shouldBe false
        }
    }

    context("a list of entity tags") {
        test("is hashed by its tags") {
            tags.hashCode() shouldBe listOf(tag).hashCode()
            tags.equals(EntityTagCondition.Any) shouldBe false
        }
    }
})
