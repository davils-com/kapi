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

package com.davils.kapi.spec.openapi.structure

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.schema.Compilation
import com.davils.kapi.spec.schema.CompiledSchema
import com.davils.kapi.spec.schema.Compiler
import com.davils.kapi.spec.schema.JsonSchema
import com.davils.kapi.spec.schema.SchemaLimits
import com.davils.kapi.spec.schema.SchemaLimitsBuilder
import kotlinx.serialization.json.Json
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val SCHEMA_30_SOURCE: String = "https://spec.openapis.org/oas/3.0/schema/2024-10-18"

private const val SCHEMA_31_SOURCE: String = "https://spec.openapis.org/oas/3.1/schema/2026-11-05"

private const val SCHEMA_32_SOURCE: String = "https://spec.openapis.org/oas/3.2/schema/2026-11-12"

private const val STEPS_PER_NODE: Long = 100L

internal class OfficialSchemas(reading: ReadLimits) {
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

    private val compiled: List<AtomicReference<CompiledSchema?>> = VersionFamily.entries.map { AtomicReference(null) }

    suspend fun of(family: VersionFamily): CompiledSchema {
        val slot = compiled[family.ordinal]
        val known = slot.load()
        if (known != null) return known
        val schema = compile(family)
        slot.compareAndSet(null, schema)
        return schema
    }

    private suspend fun compile(family: VersionFamily): CompiledSchema {
        val json = Json.parseToJsonElement(textOf(family))
        val compilation = compiler.compile(json, DocumentUri(sourceOf(family)))
        val schema = (compilation as? Compilation.Compiled)?.schema
        return checkNotNull(schema) { "The official schema of OpenAPI ${family.prefix} does not compile: $compilation" }
    }

    private fun textOf(family: VersionFamily): String = when (family) {
        VersionFamily.V30 -> SCHEMA_30
        VersionFamily.V31 -> SCHEMA_31
        VersionFamily.V32 -> SCHEMA_32
    }

    private fun sourceOf(family: VersionFamily): String = when (family) {
        VersionFamily.V30 -> SCHEMA_30_SOURCE
        VersionFamily.V31 -> SCHEMA_31_SOURCE
        VersionFamily.V32 -> SCHEMA_32_SOURCE
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
