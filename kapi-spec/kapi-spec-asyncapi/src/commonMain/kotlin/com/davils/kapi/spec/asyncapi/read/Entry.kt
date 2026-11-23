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

import com.davils.kaml.core.node.YamlNode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode

internal class Entry(
    val key: String,
    private val keyNode: YamlNode,
    val value: Cursor,
) {
    fun report(code: ViolationCode, message: String) {
        val document = value.document
        val place = document.locate(value.pointer, keyNode.range)
        value.scope.report(code = code, message = message, location = place, snippet = document.snippet(place))
    }
}
