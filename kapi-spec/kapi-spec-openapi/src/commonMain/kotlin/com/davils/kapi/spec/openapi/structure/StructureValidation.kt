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

package com.davils.kapi.spec.openapi.structure

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.at
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.schema.OutputUnit
import com.davils.kapi.spec.schema.Validation
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

internal class StructureValidation(
    private val findings: Findings,
    private val document: LoadedDocument,
    private val family: VersionFamily,
) {
    private val source: String = "The schema of OpenAPI ${family.prefix}"

    suspend fun check(schemas: OfficialSchemas) {
        val schema = schemas.of(family)
        val reported = PointerTree(findings.pointersIn(document.uri))
        when (val validation = schema.validate(document.root)) {
            is Validation.Valid -> Unit
            is Validation.Invalid -> invalid(validation.errors, reported)
            is Validation.Refused -> refused(validation, reported)
        }
    }

    private fun invalid(errors: List<OutputUnit>, reported: PointerTree) {
        val fresh = errors.filterNot { error -> reported.isRelated(error.instanceLocation) }
        val places = PointerTree(fresh.map(OutputUnit::instanceLocation))
        val deepest = fresh.filterNot { error -> places.hasBelow(error.instanceLocation) }
        val seen = OrderedSet<String>()
        val distinct = deepest.filter { error -> seen.add(error.instanceLocation.text) }
        distinct.forEach { error -> report(error.instanceLocation, "$source says: ${error.error}") }
    }

    private fun refused(refusal: Validation.Refused, reported: PointerTree) {
        if (reported.isRelated(refusal.instanceLocation)) return
        report(refusal.instanceLocation, "$source could not check the document: ${refusal.message}")
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
