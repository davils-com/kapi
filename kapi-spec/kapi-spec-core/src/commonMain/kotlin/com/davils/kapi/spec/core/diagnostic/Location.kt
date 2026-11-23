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

package com.davils.kapi.spec.core.diagnostic

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.text.TextPosition
import com.davils.kore.core.text.TextRange

public class Location(
    public val document: DocumentUri,
    public val pointer: JsonPointer,
    public val range: TextRange? = null,
    public val position: TextPosition? = null,
) {
    private val identity: List<Any?> get() = listOf(document, pointer, range, position)

    override fun equals(other: Any?): Boolean = other is Location && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String {
        val place = position ?: return "$document#$pointer"
        return "$document:$place#$pointer"
    }
}
