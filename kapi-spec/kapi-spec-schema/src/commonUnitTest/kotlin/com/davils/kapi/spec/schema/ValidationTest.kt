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

package com.davils.kapi.spec.schema

import com.davils.kaml.document.pointer.JsonPointer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

class ValidationTest : FunSpec({
    test("an output unit names the keyword through every reference, its absolute location and the value") {
        val compiled = schemaOf(
            """{"${'$'}id": "https://example.com/pet", "properties": {"name": {"${'$'}ref": "#/${'$'}defs/name"}},
               "${'$'}defs": {"name": {"type": "string"}}}""",
        )
        val error = compiled.errorsOf("""{"name": 1}""").single()
        error.keywordLocation.text shouldBe "/properties/name/\$ref/type"
        error.absoluteKeywordLocation shouldBe "https://example.com/pet#/\$defs/name/type"
        error.instanceLocation shouldBe JsonPointer.of("name")
        error.error shouldContain "string"
        error.toString() shouldContain "/name"
    }

    test("the basic format lists every failing keyword") {
        val compiled = schemaOf("""{"type": "object", "required": ["a", "b"], "properties": {"c": {"minimum": 3}}}""")
        compiled.errorsOf("""{"c": 1}""") shouldHaveSize 3
    }

    test("the flag format reports no errors and stops early") {
        val compiled = schemaOf("""{"required": ["a", "b"]}""")
        val validation = compiled.validate(json("{}"), OutputFormat.Flag)
        validation.shouldBeInstanceOf<Validation.Invalid>().errors shouldBe emptyList()
        compiled.validate(json("""{"a": 1, "b": 2}"""), OutputFormat.Flag) shouldBe Validation.Valid
    }

    test("anyOf reports the errors of every branch, a successful branch none") {
        val compiled = schemaOf("""{"anyOf": [{"type": "string"}, {"minimum": 5}]}""")
        compiled.errorsOf("1").map { it.keywordLocation.text } shouldBe listOf("/anyOf/0/type", "/anyOf/1/minimum", "/anyOf")
        compiled.accepts("7") shouldBe true
    }

    test("oneOf tells a value that matches none from one that matches several") {
        val compiled = schemaOf("""{"oneOf": [{"type": "integer"}, {"minimum": 0}]}""")
        compiled.errorsOf("1").single().error shouldContain "0 and 1"
        compiled.errorsOf("-1.5").map { it.keywordLocation.text } shouldBe listOf("/oneOf/0/type", "/oneOf/1/minimum", "/oneOf")
        compiled.accepts("-1") shouldBe true
    }

    test("untrusted names are escaped in messages and in toString") {
        val compiled = schemaOf("""{"required": ["a\u0000\"b"]}""")
        val error = compiled.errorsOf("{}").single()
        error.error shouldNotContain "\u0000"
        compiled.validate(json("{}")).toString() shouldNotContain "\u0000"
    }

    test("a YAML value validates like its JSON") {
        val compiled = schemaOf("""{"type": "object", "properties": {"200": {"type": "integer"}}}""")
        compiled.validate(yamlOf("200: 5\n")).isValid shouldBe true
        compiled.validate(yamlOf("200: five\n")).isValid shouldBe false
        compiled.validate(yamlOf("200: .nan\n")).shouldBeInstanceOf<Validation.Refused>().reason shouldBe
            RefusalReason.UnsupportedValue
    }

    test("a number kapi does not read is refused") {
        val compiled = schemaOf("{}")
        val refusal = compiled.refusalOf(json("[" + "1".repeat(5000) + "]"))
        refusal.reason shouldBe RefusalReason.UnsupportedValue
        refusal.instanceLocation.text shouldBe "/0"
        refusal.isValid shouldBe false
        refusal.toString() shouldContain "UnsupportedValue"
    }

    test("results compare by value") {
        val compiled = schemaOf("""{"type": "string"}""")
        compiled.validate(json("1")) shouldBe compiled.validate(json("1"))
        compiled.validate(json("1")).hashCode() shouldBe compiled.validate(json("1")).hashCode()
        compiled.validate(json("\"a\"")).toString() shouldBe "Valid"
    }
})
