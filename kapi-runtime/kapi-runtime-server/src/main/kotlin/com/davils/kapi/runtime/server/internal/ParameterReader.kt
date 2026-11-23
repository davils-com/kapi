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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterReading
import com.davils.kapi.runtime.core.parameter.ParameterShape
import com.davils.kapi.runtime.core.parameter.readCookie
import com.davils.kapi.runtime.core.parameter.readEntries
import com.davils.kapi.runtime.core.parameter.readHeader
import com.davils.kapi.runtime.core.parameter.readPath
import com.davils.kapi.runtime.server.request.Place
import com.davils.kapi.runtime.server.request.Request
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

private const val COOKIE: String = "cookie"

internal class ParameterReader(private val request: Request) {
    private val entries: Map<String, List<String>> by lazy { grouped(request.query) }

    fun read(
        place: Place,
        format: ParameterFormat,
        shape: ParameterShape,
        fields: Set<String> = emptySet(),
    ): ParameterReading = when (place) {
        Place.Path -> format.readPath(request.pathParameters[format.name], shape)

        Place.Query -> format.readEntries(entries = entries, shape = shape, fields = fields)

        Place.Header -> format.readHeader(joined(name = format.name, separator = ", "), shape)

        Place.Cookie -> format.readCookie(
            header = joined(name = COOKIE, separator = "; "),
            shape = shape,
            fields = fields,
        )
    }

    private fun joined(name: String, separator: String): String? {
        val values = request.headers.all(name)
        return values.joinToString(separator).takeIf { values.isNotEmpty() }
    }

    private fun grouped(query: List<Pair<String, String>>): Map<String, List<String>> {
        val grouped = OrderedMap<String, MutableList<String>>()
        query.forEach { (name, value) -> grouped.getOrPut(name) { mutableListOf() } += value }
        return grouped.entries.toMap()
    }
}
