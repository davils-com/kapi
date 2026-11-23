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

package com.davils.kapi.codegen.core.asyncapi.schema

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.codegen.core.asyncapi.GenerationCodes
import com.davils.kapi.codegen.core.asyncapi.Report
import com.davils.kapi.codegen.core.asyncapi.plan.PayloadSource
import com.davils.kapi.codegen.core.asyncapi.read.Targets
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.core.value.Value

private const val COMPONENTS: String = "components"

private const val SCHEMAS: String = "schemas"

private const val LOCAL: String = "#/components/schemas/"

private val structural: Set<String> = setOf(COMPONENTS, SCHEMAS, "channels", "messages")

private val modelled: Set<SchemaFormat> = setOf(SchemaFormat.AsyncApi, SchemaFormat.JsonSchema, SchemaFormat.OpenApi)

internal class SchemaHoist(private val document: Document, private val report: Report) {
    private val targets: Targets = Targets(document)

    private val keys: MutableMap<Target, String> = linkedMapOf()

    private val pending: ArrayDeque<Pair<String, Schema>> = ArrayDeque()

    private val origins: MutableMap<String, Target> = linkedMapOf()

    fun hoist(payloads: List<PayloadSource.Declared>): List<Hoisted> {
        val origin = targets.origin
        val components = document.components?.schemas.orEmpty()
        components.forEach { (key, referable) ->
            val schema = document.resolve(referable)?.takeIf { found -> found.format in modelled }
            val target = origin?.let { uri -> Target(uri, JsonPointer.of(COMPONENTS, SCHEMAS, key)) }
            if (schema != null && target != null) claim(target, schema, key)
        }
        payloads.filter { payload -> payload.schema.format in modelled }.forEach { payload ->
            keyOf(payload.target, payload.schema)
        }
        val hoisted = mutableListOf<Hoisted>()
        while (pending.isNotEmpty()) {
            val (key, schema) = pending.removeFirst()
            val translation = SchemaTranslation(report, schema.format, ::reference)
            hoisted += Hoisted(
                key = key,
                origin = origins.getValue(key),
                schema = schema,
                translated = translation.schema(schema.definition),
            )
        }
        return hoisted
    }

    fun keyOf(target: Target): String? = keys[target]

    private fun keyOf(target: Target, schema: Schema): String = keys[target] ?: claim(target, schema, derived(target))

    private fun claim(target: Target, schema: Schema, key: String): String {
        keys[target] = key
        origins[key] = target
        pending.addLast(key to schema)
        return key
    }

    private fun derived(target: Target): String {
        val isForeign = target.document != targets.origin
        val name = target.document.value.substringAfterLast('/')
        val file = name.substringBefore('.')
        val words = listOf(file).filter { isForeign } + target.pointer.tokens.filterNot { token -> token in structural }
        val base = Identifiers.type(words.joinToString(" "))
        val taken = origins.keys
        var candidate = base
        var counter = 2
        while (candidate in taken) {
            candidate = base + counter
            counter += 1
        }
        return candidate
    }

    private fun reference(text: Value.Text): String? {
        val base = text.location?.document ?: targets.origin
        val target = base?.let { uri -> targets.targetOf(text.value, uri) }
        val schema = target?.let(targets::schemaAt)?.takeIf { found -> found.format in modelled }
        if (schema == null) {
            val message = "The reference ${shown(text.value)} names no schema the reader resolved"
            report.error(GenerationCodes.REFERENCE_UNSUPPORTED, message, text.location)
            return null
        }
        return LOCAL + keyOf(target, schema)
    }
}
