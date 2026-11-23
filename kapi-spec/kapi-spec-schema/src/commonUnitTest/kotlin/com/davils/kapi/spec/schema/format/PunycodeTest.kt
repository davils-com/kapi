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

package com.davils.kapi.spec.schema.format

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val samples: List<Pair<String, String>> = listOf(
    "bücher" to "bcher-kva",
    "münchen" to "mnchen-3ya",
    "他们为什么不说中文" to "ihqwcrb4cv8a8dqg056pqjye",
    "ليهمابتكلموشعربي؟" to
        "egbpdaj6bu4bxfgehfvwxn",
    "😀" to "e28h",
    "abc" to "abc-",
)

class PunycodeTest : FunSpec({
    context("the samples of RFC 3492") {
        samples.forEachIndexed { index, (unicode, ascii) ->
            test("sample $index encodes and decodes") {
                Punycode.encode(codePoints(unicode)) shouldBe ascii
                Punycode.decode(ascii)?.toList() shouldBe codePoints(unicode).toList()
            }
        }
    }

    context("malformed Punycode") {
        test("is refused") {
            Punycode.decode("bü-kva") shouldBe null
            Punycode.decode("bcher-!") shouldBe null
            Punycode.decode("bcher-kv") shouldBe null
            Punycode.decode("zzzzzzzzzzzzzzzzzzzz") shouldBe null
            Punycode.decode("99999999") shouldBe null
        }

        test("decodes uppercase digits like lowercase ones") {
            val decoded = Punycode.decode("BCHER-KVA")

            decoded?.toList() shouldBe codePoints("BüCHER").toList()
        }
    }
})
