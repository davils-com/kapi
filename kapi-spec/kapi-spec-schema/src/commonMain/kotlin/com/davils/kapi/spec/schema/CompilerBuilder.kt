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

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlinx.serialization.json.JsonElement

/**
 * Configures a [Compiler] in the block of [JsonSchema.compiler].
 *
 * A builder is not thread-safe; it is meant for one block of the DSL.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class CompilerBuilder internal constructor() : ValidatingBuilder<Compiler>() {
    private var limits: SchemaLimits? = null

    private val schemas = ArrayList<Pair<DocumentUri, JsonElement>>()

    override val subject: String get() = "json schema compiler"

    /**
     * The dialect of a schema without `$schema`; [Dialect.Draft202012] by default.
     *
     * @since 1.0.0
     */
    public var defaultDialect: Dialect = Dialect.Draft202012

    /**
     * Whether `format` asserts, so that a string that is no valid date, for instance, is not valid; `false` by
     * default, which keeps `format` an annotation as JSON Schema 2020-12 asks. A meta-schema with the format-assertion
     * vocabulary turns it on for the schemas that use it.
     *
     * @since 1.0.0
     */
    public var isFormatAsserted: Boolean = false

    /**
     * Loads the documents that a schema refers to and the compiler does not know; `null` by default, which leaves
     * every unknown reference unresolved.
     *
     * @since 1.0.0
     */
    public var loader: SchemaLoader? = null

    /**
     * Configures the limits; [SchemaLimits.Default] when not called.
     *
     * @param block the limits.
     * @throws IllegalStateException if the compiler was built already.
     * @since 1.0.0
     */
    public fun limits(block: SchemaLimitsBuilder.() -> Unit) {
        limits = nested(SchemaLimitsBuilder(), Compiler::limits, block)
    }

    /**
     * Registers the schema document [json] under [uri], so that references to [uri] resolve to it without a loader.
     *
     * The document is read only when a schema refers to it, and its `$id`s apply as in every other document.
     *
     * @param uri the URI of the document.
     * @param json the document.
     * @throws IllegalStateException if the compiler was built already.
     * @since 1.0.0
     */
    public fun schema(uri: DocumentUri, json: JsonElement) {
        checkOpen()
        schemas += uri to json
    }

    override fun assemble(): Compiler = Compiler(
        defaultDialect = defaultDialect,
        isFormatAsserted = isFormatAsserted,
        limits = limits ?: SchemaLimits.Default,
        schemas = schemas.toList(),
        loader = loader,
    )
}
