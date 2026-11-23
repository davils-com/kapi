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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.schema

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.schema.internal.Refusal
import com.davils.kapi.spec.schema.internal.Trail
import com.davils.kapi.spec.schema.internal.Uris
import com.davils.kapi.spec.schema.internal.compile.Driver
import com.davils.kapi.spec.schema.internal.value.YamlReader
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode
import kotlinx.serialization.json.JsonElement

/**
 * Compiles JSON Schemas of every [Dialect] into [CompiledSchema]s, created by [JsonSchema.compiler].
 *
 * A compilation resolves every reference before any value is validated: to the schema itself and its embedded
 * resources by `$id`, `$anchor`, `$dynamicAnchor` and JSON Pointer, to the built-in meta-schemas of every dialect,
 * to the documents registered with [CompilerBuilder.schema] and, last, to what the [SchemaLoader] loads. kapi reads
 * neither the network nor the file system, so an unknown remote reference is a compile error unless the caller
 * supplies the document. The dialect of a schema comes from its `$schema` or from [defaultDialect]; a custom
 * meta-schema is loaded like any document, and its `$vocabulary` decides the keywords that apply.
 *
 * Schemas may come from an attacker: every compilation is bounded by [limits], every name and URI in a diagnostic is
 * escaped, and patterns are checked before they run. A compiler is immutable and safe to share between threads;
 * every compilation has its own state.
 *
 * @property defaultDialect the dialect of a schema without `$schema`.
 * @property formatAssertion whether `format` asserts.
 * @property limits the limits of compilation and of validation.
 * @param schemas the documents registered by URI.
 * @param loader the loader of unknown documents.
 * @since 1.0.0
 */
public class Compiler internal constructor(
    public val defaultDialect: Dialect,
    public val formatAssertion: Boolean,
    public val limits: SchemaLimits,
    schemas: List<Pair<DocumentUri, JsonElement>>,
    internal val loader: SchemaLoader?,
) : Validatable {
    private val uris: List<String> = schemas.map { (uri, _) -> Uris.normalized(uri.value) ?: uri.value }

    internal val registry: OrderedMap<String, JsonElement> = OrderedMap<String, JsonElement>().also { registry ->
        schemas.forEachIndexed { index, (_, json) -> registry[uris[index]] = json }
    }

    /**
     * Compiles [schema].
     *
     * @param schema the schema, which may come from an attacker.
     * @param uri the URI the schema was retrieved from, which is its base URI unless it has an `$id`; without one,
     * relative references resolve against `memory:/document`.
     * @return the compiled schema, or every problem found, located by JSON Pointer.
     * @throws kotlinx.coroutines.CancellationException if the calling coroutine is cancelled while the loader runs.
     * @since 1.0.0
     */
    public suspend fun compile(schema: JsonElement, uri: DocumentUri? = null): Compilation = Driver(
        this,
    ).compile(schema, uri)

    /**
     * Compiles the YAML node [schema], converted into JSON as [JsonSchema.json] does, within
     * [SchemaLimits.maximumSchemaDepth] and [SchemaLimits.maximumInstanceNodes].
     *
     * @param schema the schema, which may come from an attacker.
     * @param uri the URI the schema was retrieved from.
     * @return the compiled schema, or every problem found; a node JSON cannot express is a
     * [DiagnosticCodes.NOT_JSON] error.
     * @throws kotlinx.coroutines.CancellationException if the calling coroutine is cancelled while the loader runs.
     * @since 1.0.0
     */
    public suspend fun compile(schema: YamlNode, uri: DocumentUri? = null): Compilation {
        val converted = try {
            YamlReader(limits.maximumSchemaDepth, limits.maximumInstanceNodes).read(schema)
        } catch (refusal: Refusal) {
            val location = Location(uri ?: DocumentUri.MEMORY, Trail.pointerOf(refusal.trail))
            return Compilation.Invalid(
                listOf(Diagnostic(DiagnosticCodes.NOT_JSON, Severity.Error, refusal.description, location)),
            )
        }
        return compile(converted, uri)
    }

    override fun ValidationScope.validate() {
        val counts = uris.groupingBy { it }.eachCount()
        val duplicates = counts.filterValues { it > 1 }
        if (duplicates.isNotEmpty()) {
            fail("registers ${duplicates.size} URIs more than once", ViolationCode.DUPLICATE)
        }
    }

    override fun toString(): String =
        "Compiler(defaultDialect=$defaultDialect, formatAssertion=$formatAssertion, schemas=${uris.size}, $limits)"
}
