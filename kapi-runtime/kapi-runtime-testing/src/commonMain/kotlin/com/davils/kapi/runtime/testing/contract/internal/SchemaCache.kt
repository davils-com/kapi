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

package com.davils.kapi.runtime.testing.contract.internal

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.runtime.testing.contract.ContractCodes
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.schema.Compilation
import com.davils.kapi.spec.schema.Compiler
import com.davils.kapi.spec.schema.CompilerBuilder
import com.davils.kapi.spec.schema.Dialect
import com.davils.kapi.spec.schema.JsonSchema
import com.davils.kapi.spec.schema.OutputFormat
import com.davils.kapi.spec.schema.Validation
import com.davils.kore.core.validation.violation.ViolationCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement

private const val REGISTRY: String = "memory:/kapi-testing/schema/"

internal class SchemaCache(document: Document, configure: CompilerBuilder.() -> Unit) {
    private val isLegacy: Boolean = document.version.family == VersionFamily.V30

    private val targets: List<Pair<Target, Schema>> = document.targets.mapNotNull { (target, element) ->
        (element as? Schema)?.let { schema -> target to schema }
    }

    private val uris: Map<Target, String> = targets
        .mapIndexed { index, (target, _) -> target to "$REGISTRY$index" }
        .toMap()

    private val json: SchemaJson = SchemaJson(isLegacy) { reference -> uris[reference.target] ?: reference.text }

    private val compiler: Compiler = JsonSchema.compiler {
        val named = document.jsonSchemaDialect
        val declared = named?.let { uri -> Dialect.entries.firstOrNull { dialect -> dialect.uri == uri } }
        defaultDialect = Dialect.OpenApi30.takeIf { isLegacy } ?: declared ?: Dialect.OpenApi31
        configure()
        targets.forEach { (target, schema) -> schema(DocumentUri(uris.getValue(target)), json.of(schema)) }
    }

    private val lock: Mutex = Mutex()

    private val compiled: MutableMap<Schema, Compilation> = mutableMapOf()

    suspend fun validate(schema: Schema, value: JsonElement, invalid: ViolationCode): List<Problem> {
        val compilation = lock.withLock { compiled.getOrPut(schema) { compiler.compile(json.of(schema)) } }
        return when (compilation) {
            is Compilation.Invalid -> compilation.diagnostics.map { diagnostic ->
                Problem(code = ContractCodes.SCHEMA_INVALID, message = diagnostic.message, instance = null)
            }

            is Compilation.Compiled -> problemsOf(compilation.schema.validate(value, OutputFormat.Basic), invalid)
        }
    }

    private fun problemsOf(validation: Validation, invalid: ViolationCode): List<Problem> = when (validation) {
        is Validation.Valid -> emptyList()

        is Validation.Invalid -> validation.errors.map { unit ->
            Problem(code = invalid, message = unit.error, instance = unit.instanceLocation)
        }

        is Validation.Refused -> listOf(
            Problem(
                code = ContractCodes.VALIDATION_REFUSED,
                message = validation.message,
                instance = validation.instanceLocation,
            ),
        )
    }

    data class Problem(val code: ViolationCode, val message: String, val instance: JsonPointer?)
}
