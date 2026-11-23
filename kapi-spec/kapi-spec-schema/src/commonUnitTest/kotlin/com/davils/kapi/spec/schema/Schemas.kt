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

import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.yaml
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

fun json(text: String): JsonElement = Json.parseToJsonElement(text)

suspend fun compile(schema: String, block: CompilerBuilder.() -> Unit = {}): Compilation =
    JsonSchema.compiler(block).compile(json(schema))

suspend fun schemaOf(schema: String, block: CompilerBuilder.() -> Unit = {}): CompiledSchema {
    val compilation = compile(schema, block)
    return checkNotNull((compilation as? Compilation.Compiled)?.schema) { "The schema did not compile: $compilation" }
}

suspend fun diagnosticsOf(schema: String, block: CompilerBuilder.() -> Unit = {}): List<Diagnostic> {
    val compilation = compile(schema, block)
    return checkNotNull((compilation as? Compilation.Invalid)?.diagnostics) { "The schema compiled: $compilation" }
}

suspend fun codesOf(schema: String, block: CompilerBuilder.() -> Unit = {}): List<ViolationCode> {
    val diagnostics = diagnosticsOf(schema, block)
    return diagnostics.map { it.code }
}

fun CompiledSchema.keywordsOf(instance: String): List<String> {
    val errors = errorsOf(instance)
    return errors.map { it.keywordLocation.text }
}

fun CompiledSchema.accepts(instance: String): Boolean = validate(json(instance)).isValid

fun CompiledSchema.errorsOf(instance: String): List<OutputUnit> {
    val validation = validate(json(instance))
    return checkNotNull((validation as? Validation.Invalid)?.errors) { "The value is not invalid: $validation" }
}

fun CompiledSchema.refusalOf(instance: JsonElement): Validation.Refused {
    val validation = validate(instance)
    return checkNotNull(validation as? Validation.Refused) { "The value is not refused: $validation" }
}

suspend fun verify(schema: String, valid: List<String>, invalid: List<String>, block: CompilerBuilder.() -> Unit = {}) {
    val compiled = schemaOf(schema, block)
    valid.forEach { instance -> withClue("$schema accepts $instance") { compiled.accepts(instance) shouldBe true } }
    invalid.forEach { instance -> withClue("$schema refuses $instance") { compiled.accepts(instance) shouldBe false } }
}

fun yamlOf(text: String): YamlNode {
    val parsed = yaml().parse(text) as YamlParseResult.Parsed
    return checkNotNull(parsed.stream.single).root
}

fun nested(open: String, center: String, close: String, depth: Int): String =
    open.repeat(depth) + center + close.repeat(depth)
