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

import com.davils.kapi.codegen.core.ModelCatalog
import com.davils.kapi.codegen.core.asyncapi.GenerationCodes
import com.davils.kapi.codegen.core.asyncapi.plan.MessagePlan
import com.davils.kapi.codegen.core.asyncapi.plan.PayloadSource
import com.davils.kapi.codegen.core.pipeline.Findings
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.core.value.Value
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName

private val TRUE: Value = Value.Flag(isTrue = true)

private const val LOCAL: String = "#/components/schemas/"

private const val MOST_DEPTH: Int = 16

private const val INT32: String = "int32"

private const val ARRAY: String = "array"

private const val INTEGER: String = "integer"

private val element: ClassName = ClassName("kotlinx.serialization.json", "JsonElement")

private val scalars: Map<String, TypeName> = mapOf(
    "string" to STRING,
    INTEGER to LONG,
    "number" to DOUBLE,
    "boolean" to BOOLEAN,
)

internal class PayloadTypes(
    private val hoist: SchemaHoist,
    hoisted: List<Hoisted>,
    private val catalog: ModelCatalog,
    private val report: Findings,
) {
    private val byKey: Map<String, Hoisted> = hoisted.associateBy(Hoisted::key)

    fun typeOf(message: MessagePlan): TypeName {
        val source = message.payload as? PayloadSource.Declared ?: return element
        val key = hoist.keyOf(source.target)
        val type = key?.let { found -> typeOfKey(found, 0) }
        if (type == null) {
            val text = "The payload of the message ${shown(message.key)} is no model, scalar or list of either"
            report.error(GenerationCodes.PAYLOAD_UNSUPPORTED, text, source.schema.location)
        }
        return type ?: element
    }

    private fun typeOfKey(key: String, depth: Int): TypeName? {
        val model = catalog.models[key]
        if (model != null) return ClassName(catalog.packageName, model.type)
        val translated = byKey[key]?.run { translated }
        return translated?.let { value -> typeOfValue(value, depth + 1) }
    }

    private fun typeOfValue(value: Value, depth: Int): TypeName? {
        val mapping = value as? Value.Mapping ?: return element.takeIf { value == TRUE }
        val text = (mapping[REFERENCE] as? Value.Text)?.value
        val type = (mapping[TYPE] as? Value.Text)?.value
        val format = (mapping[FORMAT] as? Value.Text)?.value
        val isShallow = depth < MOST_DEPTH
        val referenced = text?.takeIf { found -> isShallow && found.startsWith(LOCAL) }?.removePrefix(LOCAL)
        val scalar = INT.takeIf { type == INTEGER && format == INT32 } ?: type?.let(scalars::get)
        val items = mapping[ITEMS]?.takeIf { isShallow && type == ARRAY }
        val listed = items?.let { found -> typeOfValue(found, depth + 1) }?.let { item -> LIST.parameterizedBy(item) }
        val named = referenced?.let { found -> typeOfKey(found, depth) }
        return element.takeIf { mapping.entries.isEmpty() } ?: named ?: scalar ?: listed
    }

    private companion object {
        const val REFERENCE: String = "\$ref"
        const val TYPE: String = "type"
        const val FORMAT: String = "format"
        const val ITEMS: String = "items"
    }
}
