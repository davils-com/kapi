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

package com.davils.kapi.codegen.core.extension

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.pipeline.Findings
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.ExtensionReading
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition

internal class ExtensionAudit(private val findings: Findings) {
    fun audit(document: Document) {
        document.walk().forEach(::element)
    }

    private fun element(element: Element) {
        element.extensions.entries.forEach { (name, value) ->
            val keyword = ExtensionCatalogue.keyword(name)
            keyword?.let { known -> known(element, known, value) } ?: unknown(name, value)
        }
        if (element is SchemaDefinition) enumNames(element)
    }

    private fun known(element: Element, keyword: ExtensionKeyword, value: Value) {
        val place = value.location
        if (ExtensionSite.of(element) !in keyword.sites) {
            val sites = keyword.sites.joinToString(" or ") { allowed -> allowed.name }
            findings.error(
                CodegenCodes.EXTENSION_SITE,
                "${keyword.name} has no meaning here, it belongs to $sites",
                place,
            )
        }
        if (!keyword.shape.accepts(value)) {
            findings.error(
                CodegenCodes.EXTENSION_VALUE,
                "${keyword.name} must be ${keyword.shape.expectation}",
                place,
            )
        }
    }

    private fun unknown(name: ExtensionName, value: Value) {
        if (!ExtensionCatalogue.isReserved(name)) return
        val reason = ExtensionCatalogue.unsupported[name.value] ?: "x-kapi- is reserved for the keywords of kide"
        findings.error(
            CodegenCodes.EXTENSION_UNKNOWN,
            "$name is no keyword of the generator: $reason",
            value.location,
        )
    }

    private fun enumNames(schema: SchemaDefinition) {
        val own = schema.extensions.texts(Keywords.ENUM_NAMES)
        val alias = schema.extensions.texts(Keywords.ENUM_NAMES_ALIAS)
        val readings = listOf(own, alias).filterIsInstance<ExtensionReading.Present<List<String>>>()
        if (readings.size > 1) {
            findings.error(
                CodegenCodes.EXTENSION_VALUE,
                "${Keywords.ENUM_NAMES} and ${Keywords.ENUM_NAMES_ALIAS} both name the entries",
                schema.location,
            )
        }
        readings.firstOrNull()?.let { names -> entryCount(schema, names.value) }
    }

    private fun entryCount(schema: SchemaDefinition, names: List<String>) {
        val entries = schema.enumeration.orEmpty()
        if (names.size == entries.size) return
        findings.error(
            CodegenCodes.EXTENSION_VALUE,
            "The schema has ${entries.size} entries but ${names.size} names for them",
            schema.location,
        )
    }
}
