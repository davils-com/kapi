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

package com.davils.kapi.spec.schema

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * One failing keyword of a [Validation], an output unit of the "basic" output format of JSON Schema 2020-12.
 *
 * Every location may contain names from the schema and the value, which may come from an attacker; [toString] shows
 * them escaped. A unit is immutable and safe to share between threads.
 *
 * @property keywordLocation the path of the failing keyword through the schema, following every `$ref`, such as
 * `/properties/name/$ref/type`.
 * @property absoluteKeywordLocation the absolute URI of the failing keyword in the schema resource that holds it,
 * such as `https://example.com/pet#/$defs/name/type`.
 * @property instanceLocation the place in the value that failed, such as `/name`.
 * @property error what failed, in English; text from the value or the schema in it is escaped.
 * @since 1.0.0
 */
public class OutputUnit internal constructor(
    public val keywordLocation: JsonPointer,
    public val absoluteKeywordLocation: String,
    public val instanceLocation: JsonPointer,
    public val error: String,
) {
    private val identity: List<Any> get() = listOf(keywordLocation, absoluteKeywordLocation, instanceLocation, error)

    override fun equals(other: Any?): Boolean = other is OutputUnit && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String =
        "${SafeText.text(instanceLocation.text)}: $error (${SafeText.text(keywordLocation.text)})"
}
