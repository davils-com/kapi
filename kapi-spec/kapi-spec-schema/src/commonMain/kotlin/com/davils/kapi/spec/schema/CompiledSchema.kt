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

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.schema.internal.Refusal
import com.davils.kapi.spec.schema.internal.Trail
import com.davils.kapi.spec.schema.internal.evaluate.Failure
import com.davils.kapi.spec.schema.internal.evaluate.Graph
import com.davils.kapi.spec.schema.internal.evaluate.Run
import com.davils.kapi.spec.schema.internal.evaluate.Visit
import com.davils.kapi.spec.schema.internal.value.InstanceReader
import com.davils.kapi.spec.schema.internal.value.YamlReader
import kotlinx.serialization.json.JsonElement

/**
 * A compiled schema that validates values, created by [Compiler.compile].
 *
 * Validation is pure computation: it reads nothing and suspends nowhere. Every validation is bounded by the
 * [SchemaLimits] of the compiler, so a hostile combination of schema and value is refused instead of running long or
 * deep. A compiled schema is immutable and safe to share between threads; validations may run at the same time.
 *
 * @property uri the base URI of the schema: its `$id`, or the URI it was compiled with.
 * @since 1.0.0
 */
public class CompiledSchema internal constructor(
    private val graph: Graph,
    private val root: Int,
    private val limits: SchemaLimits,
    public val uri: String,
) {
    /**
     * The number of subschemas compiled, the referenced documents and meta-schemas included.
     *
     * @since 1.0.0
     */
    public val subschemas: Int get() = graph.nodes.size

    /**
     * Validates [instance].
     *
     * @param instance the value, which may come from an attacker.
     * @param output how much to report.
     * @return [Validation.Valid], [Validation.Invalid] with the failing keywords, or [Validation.Refused] when a limit
     * stopped the validation or the value holds a number kapi does not read.
     * @since 1.0.0
     */
    public fun validate(instance: JsonElement, output: OutputFormat = OutputFormat.Basic): Validation = try {
        validated(InstanceReader(limits.maximumInstanceDepth).read(instance), output)
    } catch (refusal: Refusal) {
        Validation.Refused(refusal.reason, refusal.description, Trail.pointerOf(refusal.trail))
    }

    /**
     * Validates the YAML node [instance], converted into JSON as [JsonSchema.json] does, within
     * [SchemaLimits.maximumInstanceDepth] and [SchemaLimits.maximumInstanceNodes].
     *
     * @param instance the value, which may come from an attacker.
     * @param output how much to report.
     * @return the validation; a node JSON cannot express is refused with [RefusalReason.UnsupportedValue].
     * @since 1.0.0
     */
    public fun validate(instance: YamlNode, output: OutputFormat = OutputFormat.Basic): Validation = try {
        val json = YamlReader(limits.maximumInstanceDepth, limits.maximumInstanceNodes).read(instance)
        validate(json, output)
    } catch (refusal: Refusal) {
        Validation.Refused(refusal.reason, refusal.description, Trail.pointerOf(refusal.trail))
    }

    private fun validated(
        instance: com.davils.kapi.spec.schema.internal.value.Instance,
        output: OutputFormat,
    ): Validation {
        val sink = ArrayList<Failure>().takeIf { output == OutputFormat.Basic }
        val result = Run(graph, limits).evaluate(root, Visit(instance, null, null, null, null), sink)
        if (result != null) return Validation.Valid
        return invalid(sink.orEmpty())
    }

    private fun invalid(failures: List<Failure>): Validation {
        if (failures.size > limits.maximumErrors) {
            throw Refusal(RefusalReason.ReportedErrors, "The value has more than ${limits.maximumErrors} errors", null)
        }
        val units = failures.map { failure ->
            OutputUnit(
                keywordLocation = Trail.pointerOf(failure.keyword),
                absoluteKeywordLocation = failure.absolute,
                instanceLocation = Trail.pointerOf(failure.instance),
                error = failure.message,
            )
        }
        return Validation.Invalid(units)
    }

    override fun toString(): String = "CompiledSchema(uri=$uri, subschemas=$subschemas)"
}
