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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kapi.spec.asyncapi.structure

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.schema.Compilation
import com.davils.kapi.spec.schema.CompiledSchema
import com.davils.kapi.spec.schema.Compiler
import com.davils.kapi.spec.schema.JsonSchema
import com.davils.kapi.spec.schema.SchemaLimits
import com.davils.kapi.spec.schema.SchemaLimitsBuilder
import kotlinx.serialization.json.Json
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val SCHEMA_300_SOURCE: String =
    "https://raw.githubusercontent.com/asyncapi/spec-json-schemas/v6.11.1/schemas/3.0.0-without-%24id.json"

private const val STEPS_PER_NODE: Long = 100L

internal class OfficialSchema(reading: ReadLimits) {
    private val compiler: Compiler = JsonSchema.compiler {
        limits {
            maximumInstanceDepth = reading.maximumDepth.coerceAtMost(SchemaLimits.CEILING_INSTANCE_DEPTH)
            maximumInstanceNodes = nodesOf(reading)
            maximumEvaluationSteps = stepsOf(reading)
            maximumEvaluationDepth = SchemaLimits.CEILING_EVALUATION_DEPTH
            maximumErrors = SchemaLimits.CEILING_ERRORS
            maximumStringLength = reading.maximumScalarLength.coerceAtMost(SchemaLimits.CEILING_STRING_LENGTH)
        }
    }

    private val compiled: AtomicReference<CompiledSchema?> = AtomicReference(null)

    suspend fun get(): CompiledSchema {
        val known = compiled.load()
        if (known != null) return known
        val schema = compile()
        compiled.compareAndSet(null, schema)
        return schema
    }

    private suspend fun compile(): CompiledSchema {
        val json = Json.parseToJsonElement(SCHEMA_300_PARTS.joinToString(separator = ""))
        val compilation = compiler.compile(json, DocumentUri(SCHEMA_300_SOURCE))
        val schema = (compilation as? Compilation.Compiled)?.schema
        return checkNotNull(schema) { "The official schema of AsyncAPI 3.0 does not compile: $compilation" }
    }

    private fun nodesOf(reading: ReadLimits): Int {
        val ceiling = SchemaLimits.CEILING_INSTANCE_NODES.toLong()
        return reading.maximumNodes.coerceAtMost(ceiling).toInt()
    }

    private fun stepsOf(reading: ReadLimits): Long {
        val steps = reading.maximumNodes * STEPS_PER_NODE
        return steps.coerceIn(
            minimumValue = SchemaLimitsBuilder.DEFAULT_EVALUATION_STEPS,
            maximumValue = SchemaLimits.CEILING_EVALUATION_STEPS,
        )
    }
}
