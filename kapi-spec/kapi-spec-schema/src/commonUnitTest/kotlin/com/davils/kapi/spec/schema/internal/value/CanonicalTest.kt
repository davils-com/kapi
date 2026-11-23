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

package com.davils.kapi.spec.schema.internal.value

import com.davils.kapi.spec.schema.RefusalReason
import com.davils.kapi.spec.schema.internal.Meter
import com.davils.kapi.spec.schema.internal.Refusal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json

private const val DEPTH: Int = 64

private fun canonical(text: String): String = Canonical.of(
    InstanceReader(DEPTH).read(Json.parseToJsonElement(text)),
    Meter { },
)

class CanonicalTest : FunSpec({
    test("equal JSON values have the same canonical form") {
        canonical("""{"a": 1, "b": [2.0, "x"]}""") shouldBe canonical("""{"b": [2, "x"], "a": 1.00}""")
        canonical("1") shouldNotBe canonical("\"1\"")
        canonical("""["ab", "c"]""") shouldNotBe canonical("""["a", "bc"]""")
        canonical("true") shouldNotBe canonical("1")
        canonical("null") shouldBe "n"
    }

    test("the reader refuses depth beyond its limit") {
        val refusal = shouldThrow<Refusal> { InstanceReader(1).read(Json.parseToJsonElement("[[1]]")) }
        refusal.reason shouldBe RefusalReason.InstanceDepth
    }

    test("objects look keys up by order, not by hash") {
        val members = InstanceReader(4).read(Json.parseToJsonElement("""{"b": 1, "a": 2}""")) as Instance.ObjectValue
        members.indexOf("a") shouldBe 1
        members.indexOf("c") shouldBe null
        members.type shouldBe "object"
    }
})
