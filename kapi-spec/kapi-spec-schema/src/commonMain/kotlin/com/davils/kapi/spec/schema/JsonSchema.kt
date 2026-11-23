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
import com.davils.kapi.spec.schema.internal.value.YamlReader

/**
 * The entry point of JSON Schema in kapi: compilers of schemas and the conversion of YAML into JSON.
 *
 * ```kotlin
 * val compiler = JsonSchema.compiler {
 *     defaultDialect = Dialect.Draft202012
 *     limits { maximumEvaluationSteps = 1_000_000L }
 * }
 * val compilation = compiler.compile(Json.parseToJsonElement(text))
 * ```
 *
 * @since 1.0.0
 */
public object JsonSchema {
    /**
     * The default most levels a YAML node of [json] may nest.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_YAML_DEPTH: Int = 128

    /**
     * The default most nodes a YAML node of [json] may have.
     *
     * @since 1.0.0
     */
    public const val DEFAULT_YAML_NODES: Int = 1_000_000

    /**
     * Creates a compiler of schemas, configured by [block].
     *
     * @param block the configuration: the default dialect, the assertion of formats, the limits, the schemas known by
     * URI and the loader of unknown ones.
     * @return the compiler, which is immutable, reusable and safe to share between threads.
     * @throws com.davils.kore.core.validation.report.ValidationException if the configuration is not valid, such as a
     * limit above its ceiling or a URI registered twice; its report names every problem.
     * @since 1.0.0
     */
    public fun compiler(block: CompilerBuilder.() -> Unit = {}): Compiler = CompilerBuilder().apply(block).build()

    /**
     * Converts a YAML node, such as an OpenAPI document read by kaml, into JSON.
     *
     * Scalars convert by their tag: `null`, booleans, integers and floats that are JSON numbers, and strings. Keys
     * become their text. A float such as `.inf`, an integer such as `0x1F`, a non-scalar key, two keys with the same
     * text and any other tag are refused, so nothing is guessed. Anchors and aliases are expanded, and every expanded
     * node counts toward [maximumNodes].
     *
     * @param node the node, which may come from an attacker.
     * @param maximumDepth the most levels the node may nest.
     * @param maximumNodes the most nodes the node may have, aliases expanded.
     * @return the JSON value, or the place and the reason the node could not be converted.
     * @since 1.0.0
     */
    public fun json(
        node: YamlNode,
        maximumDepth: Int = DEFAULT_YAML_DEPTH,
        maximumNodes: Int = DEFAULT_YAML_NODES,
    ): YamlConversion = try {
        YamlConversion.Converted(YamlReader(maximumDepth, maximumNodes).read(node))
    } catch (refusal: Refusal) {
        YamlConversion.Refused(Trail.pointerOf(refusal.trail), refusal.description)
    }
}
