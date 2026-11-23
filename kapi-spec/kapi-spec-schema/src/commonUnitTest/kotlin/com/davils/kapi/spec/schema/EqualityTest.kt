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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val REASON: String = "too deep"

private const val WRONG_TYPE: String = """{"type": 1}"""

class EqualityTest : FunSpec({
    test("a compilation equals another only when it holds the same result") {
        val schema = schemaOf("{}")
        val compiled = Compilation.Compiled(schema)
        compiled shouldBe Compilation.Compiled(schema)
        compiled shouldNotBe Compilation.Compiled(schemaOf("{}"))
        compiled.equals(REASON) shouldBe false
        compiled.hashCode() shouldBe Compilation.Compiled(schema).hashCode()
        compiled.toString() shouldContain "Compiled"
        val invalid = compile(WRONG_TYPE)
        invalid shouldBe compile(WRONG_TYPE)
        invalid shouldNotBe compile("""{"type": 2, "minLength": -1}""")
        invalid.equals(compiled) shouldBe false
        invalid.hashCode() shouldBe compile(WRONG_TYPE).hashCode()
    }

    test("a conversion equals another with the same value or the same refusal") {
        val converted = JsonSchema.json(yamlOf("a: 1"))
        converted shouldBe JsonSchema.json(yamlOf("a: 1"))
        converted shouldNotBe JsonSchema.json(yamlOf("a: 2"))
        converted.equals(REASON) shouldBe false
        converted.hashCode() shouldBe JsonSchema.json(yamlOf("a: 1")).hashCode()
        val root = JsonPointer.of(emptyList())
        val refused = YamlConversion.Refused(root, REASON)
        refused shouldBe YamlConversion.Refused(root, REASON)
        refused shouldNotBe YamlConversion.Refused(JsonPointer.of(listOf("a")), REASON)
        refused shouldNotBe YamlConversion.Refused(root, "too wide")
        refused.equals(converted) shouldBe false
        refused.hashCode() shouldBe YamlConversion.Refused(root, REASON).hashCode()
    }

    test("a validation equals another with the same errors or the same refusal") {
        val root = JsonPointer.of(emptyList())
        val unit = OutputUnit(
            keywordLocation = root,
            absoluteKeywordLocation = "memory:/document#",
            instanceLocation = root,
            error = REASON,
        )
        val invalid = Validation.Invalid(listOf(unit))
        invalid shouldBe Validation.Invalid(listOf(unit))
        invalid shouldNotBe Validation.Invalid(emptyList())
        invalid.equals(Validation.Valid) shouldBe false
        unit.equals(REASON) shouldBe false
        val refused = Validation.Refused(RefusalReason.InstanceDepth, REASON, root)
        refused shouldBe Validation.Refused(RefusalReason.InstanceDepth, REASON, root)
        refused shouldNotBe Validation.Refused(RefusalReason.InstanceSize, REASON, root)
        refused.equals(invalid) shouldBe false
        refused.hashCode() shouldBe Validation.Refused(RefusalReason.InstanceDepth, REASON, root).hashCode()
        SchemaLimits.Default.equals(REASON) shouldBe false
    }
})
