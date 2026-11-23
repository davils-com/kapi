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

package com.davils.kapi.spec.asyncapi.structure

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.at
import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.diagnostic.Findings
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.core.diagnostic.PointerTree
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.schema.OutputUnit
import com.davils.kapi.spec.schema.Validation
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

private const val SOURCE: String = "The schema of AsyncAPI 3.0"

private const val REFERENCE: String = "\$ref"

private val interpreted: Set<SchemaFormat> = setOf(SchemaFormat.AsyncApi, SchemaFormat.JsonSchema, SchemaFormat.OpenApi)

internal class StructureValidation(
    private val findings: Findings,
    private val document: LoadedDocument,
    model: Document,
) {
    private val booleans: OrderedSet<String> = OrderedSet(placesOf(model) { schema -> schema.definition is Value.Flag })

    private val opaque: OrderedSet<String> = OrderedSet(placesOf(model) { schema -> schema.format !in interpreted })

    suspend fun check(schema: OfficialSchema) {
        val compiled = schema.get()
        val reported = PointerTree(findings.pointersIn(document.uri))
        when (val validation = compiled.validate(document.root)) {
            is Validation.Valid -> Unit
            is Validation.Invalid -> invalid(validation.errors, reported)
            is Validation.Refused -> refused(validation, reported)
        }
    }

    private fun invalid(errors: List<OutputUnit>, reported: PointerTree) {
        val fresh = errors.filterNot { error -> reported.isRelated(error.instanceLocation) }
        val places = PointerTree(fresh.map(OutputUnit::instanceLocation))
        val deepest = fresh.filterNot { error -> places.hasBelow(error.instanceLocation) || isResolved(error) }
        val seen = OrderedSet<String>()
        val distinct = deepest.filter { error -> seen.add(error.instanceLocation.text) }
        distinct.forEach { error -> report(error.instanceLocation, "$SOURCE says: ${error.error}") }
    }

    private fun isResolved(error: OutputUnit): Boolean {
        val pointer = error.instanceLocation
        val node = document.root.at(pointer) as? YamlNode.Mapping
        val isReference = node?.get(REFERENCE)?.string() != null
        return isReference || pointer.text in booleans || isOpaque(pointer)
    }

    private fun isOpaque(pointer: JsonPointer): Boolean {
        val prefixes = pointer.tokens.runningFold(JsonPointer.ROOT) { prefix, token -> prefix.child(token) }
        return prefixes.any { prefix -> prefix.text in opaque }
    }

    private fun placesOf(model: Document, isLeftOut: (Schema) -> Boolean): List<String> {
        val schemas = model.walk().filterIsInstance<Schema>()
        val leftOut = schemas.filter(isLeftOut)
        val locations = leftOut.mapNotNull(Schema::location)
        val inRoot = locations.filter { location -> location.document == document.uri }
        return inRoot.map { location -> location.pointer.text }.toList()
    }

    private fun refused(refusal: Validation.Refused, reported: PointerTree) {
        if (reported.isRelated(refusal.instanceLocation)) return
        report(refusal.instanceLocation, "$SOURCE could not check the document: ${refusal.message}")
    }

    private fun report(pointer: JsonPointer, message: String) {
        val location = document.locate(pointer, document.root.at(pointer))
        findings.report(
            code = DiagnosticCodes.STRUCTURE_SCHEMA,
            message = message,
            location = location,
            snippet = document.snippet(location),
        )
    }
}
