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

package com.davils.kapi.spec.core.load

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.text.TextSnippet

/**
 * A document a reading loaded: its text, its parsed root and where it came from, with the means to locate places in
 * it.
 *
 * @property text the text of the document.
 * @property root the root node of the document.
 * @property source where the document came from.
 * @since 1.0.0
 */
@DavilsInternal
public class LoadedDocument(
    public val text: String,
    public val root: YamlNode,
    public val source: SourceDocument,
) {
    private val lines: LineMap = LineMap.of(text)

    /**
     * The URI of the document.
     *
     * @since 1.0.0
     */
    public val uri: DocumentUri get() = source.uri

    /**
     * Returns the location of [node] at [pointer].
     *
     * @param pointer the place of the node.
     * @param node the node, whose range gives the line and column, or `null`.
     * @return the location.
     * @since 1.0.0
     */
    public fun locate(pointer: JsonPointer, node: YamlNode?): Location = locate(pointer, node?.range)

    /**
     * Returns the location of [range] at [pointer].
     *
     * @param pointer the place.
     * @param range the range of the text, or `null`.
     * @return the location.
     * @since 1.0.0
     */
    public fun locate(pointer: JsonPointer, range: TextRange?): Location {
        if (range == null) return Location(uri, pointer)
        return Location(
            document = uri,
            pointer = pointer,
            range = range,
            position = lines.positionOf(range.start),
        )
    }

    /**
     * Returns the line of the source around [location].
     *
     * @param location the location.
     * @return the snippet, or `null` if the location has no range.
     * @since 1.0.0
     */
    public fun snippet(location: Location): TextSnippet? = location.range?.let(lines::snippet)
}
