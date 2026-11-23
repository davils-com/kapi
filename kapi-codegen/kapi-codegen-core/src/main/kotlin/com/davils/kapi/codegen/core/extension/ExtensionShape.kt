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

import com.davils.kapi.spec.core.value.Value

internal enum class ExtensionShape(val expectation: String, private val check: (Value) -> Boolean) {
    Flag("true or false", ExtensionValues::isFlag),
    Identifier("a Kotlin identifier", ExtensionValues::isIdentifier),
    TypeReference("a qualified type name or a mapping with type and serializer", ExtensionValues::isTypeReference),
    Identifiers("a list of distinct Kotlin identifiers", ExtensionValues::isIdentifiers),
    Label("a text that is not blank", ExtensionValues::isLabel),
    Timeout(
        "an ISO 8601 duration above zero or a mapping of request, connect and socket to one",
        ExtensionValues::isTimeout,
    ),
    Stream("records, events or bytes", ExtensionValues::isStream),
    Pagination(
        "a mapping with the style cursor, offset, page or link and the texts parameter, items, next and size",
        ExtensionValues::isPagination,
    ),
    ;

    fun accepts(value: Value): Boolean = check(value)
}
