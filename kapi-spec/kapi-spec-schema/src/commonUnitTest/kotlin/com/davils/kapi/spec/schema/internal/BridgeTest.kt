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
import io.kotest.matchers.shouldBe

class BridgeTest : FunSpec({
    test("the format bridge knows its subset and nothing else") {
        FormatBridge.check("date", "2000-02-29") shouldBe true
        FormatBridge.check("date", "1900-02-29") shouldBe false
        FormatBridge.check("date", "2023-04-31") shouldBe false
        FormatBridge.check("date-time", "2023-04-30T12:00:00-05:30") shouldBe true
        FormatBridge.check("date-time", "2023-04-30T23:59:60-01:00") shouldBe false
        FormatBridge.check("date-time", "2023-04-30T22:59:60-01:00") shouldBe true
        FormatBridge.check("date-time", "2023-04-30T24:00:00Z") shouldBe false
        FormatBridge.check("date-time", "2023-04-30T12:00:00+25:00") shouldBe false
        FormatBridge.check("ipv4", "0.0.0.0") shouldBe true
        FormatBridge.check("ipv4", "1.2.3.4.5") shouldBe false
        FormatBridge.check("uuid", "00000000-0000-0000-0000-00000000000g") shouldBe false
        FormatBridge.check("email", "a.b@c") shouldBe true
        FormatBridge.check("email", ".a@c") shouldBe false
        FormatBridge.check("int32", "1") shouldBe null
    }

    test("the pattern bridge refuses what it cannot compile") {
        PatternBridge.compile("(") shouldBe null
        checkNotNull(PatternBridge.compile("^\\p{Letter}+$"))("abc") shouldBe true
    }

    test("URIs resolve, normalize and decode") {
        Uris.resolve("b.json#/x", "https://example.com/a/c.json") shouldBe "https://example.com/a/b.json#/x"
        Uris.resolve("#/x", "urn:uuid:deadbeef-1234-ffff-ffff-4321feebdaed") shouldBe
            "urn:uuid:deadbeef-1234-ffff-ffff-4321feebdaed#/x"
        Uris.resolve("not a uri", "https://example.com/") shouldBe null
        Uris.normalized("http://json-schema.org/draft-07/schema#") shouldBe "http://json-schema.org/draft-07/schema"
        Uris.normalized("relative") shouldBe null
        Uris.pointerOf("/a%25b/c~1d") shouldBe listOf("a%b", "c/d")
        Uris.percentDecoded("%zz") shouldBe null
        Uris.percentDecoded("%C3%A9") shouldBe "é"
    }
})
