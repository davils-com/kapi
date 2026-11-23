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

package com.davils.kapi.runtime.testing.fake

import com.davils.kapi.runtime.core.security.Secret
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FakeTokenSourceTest : FunSpec({
    context("a fake token source") {
        test("hands out its token and counts the requests") {
            val source = FakeTokenSource("t1")

            source.token() shouldBe Secret("t1")
            source.token() shouldBe Secret("t1")

            source.tokenRequests shouldBe 2
            source.refreshRequests shouldBe 0
            source.current shouldBe "t1"
            source.toString() shouldBe "FakeTokenSource(token=****, tokenRequests=2, refreshRequests=0)"
        }

        test("follows its script of refreshes and refuses once it is used up") {
            val script = listOf(RefreshOutcome.Rotate("t2"), RefreshOutcome.Keep, RefreshOutcome.Refuse)
            val source = FakeTokenSource("t1", script)

            val outcomes = listOf(
                source.refresh(Secret("t1")),
                source.refresh(null),
                source.refresh(Secret("t2")),
                source.refresh(Secret("t2")),
            )

            outcomes shouldBe listOf(true, true, false, false)
            source.current shouldBe "t2"
            source.refreshRequests shouldBe outcomes.size
            source.rejected shouldBe listOf(Secret("t1"), null, Secret("t2"), Secret("t2"))
        }

        test("throws for a scripted failure and has no token without one") {
            val source = FakeTokenSource(null, listOf(RefreshOutcome.Fail("broken")))

            source.token() shouldBe null
            shouldThrow<IllegalStateException> { source.refresh(null) }.message shouldBe "Fail(\"broken\")"
            source.toString() shouldBe "FakeTokenSource(token=none, tokenRequests=1, refreshRequests=1)"
        }
    }

    context("the outcomes of a refresh") {
        test("are values that never show a token") {
            RefreshOutcome.Rotate("t") shouldBe RefreshOutcome.Rotate("t")
            RefreshOutcome.Rotate("t").hashCode() shouldBe RefreshOutcome.Rotate("t").hashCode()
            RefreshOutcome.Rotate("t") shouldNotBe RefreshOutcome.Rotate("u")
            RefreshOutcome.Rotate("t").toString() shouldBe "Rotate(****)"
            RefreshOutcome.Fail("m") shouldBe RefreshOutcome.Fail("m")
            RefreshOutcome.Fail("m").hashCode() shouldBe RefreshOutcome.Fail("m").hashCode()
            RefreshOutcome.Fail("m") shouldNotBe RefreshOutcome.Fail("n")
            RefreshOutcome.Rotate("t").equals(RefreshOutcome.Keep) shouldBe false
            RefreshOutcome.Fail("m").equals(RefreshOutcome.Refuse) shouldBe false
            RefreshOutcome.Keep.toString() shouldBe "Keep"
            RefreshOutcome.Refuse.toString() shouldBe "Refuse"
        }
    }
})
