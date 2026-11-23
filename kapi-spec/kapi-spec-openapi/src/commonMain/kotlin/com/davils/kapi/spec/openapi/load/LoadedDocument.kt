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

package com.davils.kapi.spec.openapi.load

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.text.TextSnippet

internal class LoadedDocument(
    val text: String,
    val root: YamlNode,
    val source: SourceDocument,
) {
    private val lines: LineMap = LineMap.of(text)

    val uri: DocumentUri get() = source.uri

    fun locate(pointer: JsonPointer, node: YamlNode?): Location = locate(pointer, node?.range)

    fun locate(pointer: JsonPointer, range: TextRange?): Location {
        if (range == null) return Location(uri, pointer)
        return Location(
            document = uri,
            pointer = pointer,
            range = range,
            position = lines.positionOf(range.start),
        )
    }

    fun snippet(location: Location): TextSnippet? = location.range?.let(lines::snippet)
}
