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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode

internal class Cursor(
    val scope: ReadScope,
    val document: LoadedDocument,
    val pointer: JsonPointer,
    val node: YamlNode,
) {
    val location: Location get() = document.locate(pointer, node)

    val isMapping: Boolean get() = node is YamlNode.Mapping

    val text: String? get() = (node as? YamlNode.Scalar)?.takeIf { scalar -> scalar.tag == YamlTag.STRING }?.value

    val isNull: Boolean get() = (node as? YamlNode.Scalar)?.tag == YamlTag.NULL

    fun child(token: String, node: YamlNode): Cursor = Cursor(
        scope = scope,
        document = document,
        pointer = pointer.child(token),
        node = node,
    )

    fun items(): List<Cursor> {
        val sequence = node as? YamlNode.Sequence ?: return emptyList()
        return sequence.items.mapIndexed { index, item -> child(index.toString(), item) }
    }

    fun entries(): List<Entry> {
        val mapping = node as? YamlNode.Mapping ?: return emptyList()
        return mapping.entries.mapNotNull(::entryOf)
    }

    fun report(code: ViolationCode, message: String) {
        scope.report(code = code, message = message, location = location, snippet = document.snippet(location))
    }

    private fun entryOf(entry: YamlEntry): Entry? {
        val key = (entry.key as? YamlNode.Scalar)?.value
        if (key == null) {
            val place = document.locate(pointer, entry.key)
            val code = DiagnosticCodes.SOURCE_KEY_NOT_SCALAR
            val message = "A key must be a scalar"
            scope.report(code = code, message = message, location = place, snippet = document.snippet(place))
        }
        return key?.let { name -> Entry(name, entry.key, child(name, entry.value)) }
    }
}
