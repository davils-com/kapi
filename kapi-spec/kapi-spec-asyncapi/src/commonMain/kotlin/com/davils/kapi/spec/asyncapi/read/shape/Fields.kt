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

package com.davils.kapi.spec.asyncapi.read.shape

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.read.Cursor
import com.davils.kapi.spec.asyncapi.read.Entry
import com.davils.kapi.spec.asyncapi.read.Values
import com.davils.kapi.spec.core.value.Extensions
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.validation.violation.ViolationCode

internal class Fields(
    val cursor: Cursor,
    private val present: Map<String, Cursor>,
    val extensions: Extensions,
) {
    val names: List<String> get() = present.keys.toList()

    fun cursor(name: String): Cursor? = present[name]

    fun text(name: String): String? = present[name]?.text

    fun value(name: String): Value? = present[name]?.let(Values::of)

    fun items(name: String): List<Cursor> = present[name]?.items().orEmpty()

    fun entries(name: String): List<Entry> = present[name]?.entries().orEmpty()

    fun texts(name: String): List<String> = items(name).mapNotNull { item -> textOf(name, item) }

    fun textsByName(name: String): Map<String, String> {
        val entries = entries(name)
        val read = entries.mapNotNull { entry -> textOf(name, entry.value)?.let { text -> entry.key to text } }
        return read.toMap()
    }

    fun report(name: String, code: ViolationCode, message: String) {
        val place = present[name] ?: cursor
        place.report(code, message)
    }

    private fun textOf(name: String, item: Cursor): String? {
        val text = item.text
        if (text == null) item.report(DiagnosticCodes.FIELD_TYPE, "A value of $name must be a string")
        return text
    }
}
