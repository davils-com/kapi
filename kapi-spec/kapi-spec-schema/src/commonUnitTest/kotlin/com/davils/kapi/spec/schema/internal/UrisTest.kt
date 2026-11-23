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

package com.davils.kapi.spec.schema.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val BASE: String = "https://example.com/a/b.json"

class UrisTest : FunSpec({
    test("a reference resolves against an absolute base") {
        Uris.resolve(reference = "c.json#/x", base = BASE) shouldBe "https://example.com/a/c.json#/x"
        Uris.resolve(reference = "../c.json", base = BASE) shouldBe "https://example.com/c.json"
    }

    test("a malformed reference or base resolves to nothing") {
        Uris.resolve(reference = "c.json", base = "https://exa mple.com/").shouldBeNull()
        Uris.resolve(reference = "c d.json", base = BASE).shouldBeNull()
    }

    test("only an absolute URI is normalized, without an empty fragment") {
        Uris.normalized("HTTPS://Example.com/a/./b.json#") shouldBe "https://example.com/a/b.json"
        Uris.normalized("a/b.json").shouldBeNull()
        Uris.normalized("https://exa mple.com/").shouldBeNull()
    }

    test("a fragment is split off") {
        Uris.withoutFragment("$BASE#/x") shouldBe BASE
        Uris.fragmentOf("$BASE#/x") shouldBe "/x"
        Uris.fragmentOf(BASE) shouldBe ""
    }

    test("a fragment is decoded into the tokens of a JSON Pointer") {
        Uris.pointerOf("/a%20b/c~1d") shouldBe listOf("a b", "c/d")
        Uris.pointerOf("/a%2").shouldBeNull()
        Uris.pointerOf("/a%zz").shouldBeNull()
        Uris.pointerOf("a").shouldBeNull()
    }

    test("percent escapes decode as UTF-8 and malformed ones are refused") {
        Uris.percentDecoded("%C3%A4%41") shouldBe "äA"
        Uris.percentDecoded("100%").shouldBeNull()
        Uris.percentDecoded("%g0").shouldBeNull()
        Uris.percentDecoded("%0g").shouldBeNull()
        Uris.percentDecoded("%gzab").shouldBeNull()
    }
})
