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

package com.davils.kapi.spec.openapi.read

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.read.reference.Referables
import com.davils.kapi.spec.openapi.read.reference.ReferenceKind
import com.davils.kapi.spec.openapi.read.shape.Fields
import com.davils.kapi.spec.openapi.read.shape.ObjectShape
import com.davils.kapi.spec.openapi.read.shape.ShapeCheck
import com.davils.kore.core.annotation.DavilsInternal

internal fun <B : ElementBuilder<*>> Cursor.fill(shape: ObjectShape, builder: B, fill: B.(Fields) -> Unit): B {
    builder.location = location
    val fields = ShapeCheck.fields(this, shape)
    fields?.let { present ->
        builder.adopt(present.extensions)
        builder.fill(present)
    }
    return builder
}

internal fun <T : Element, B : ElementBuilder<T>> Cursor.read(
    shape: ObjectShape,
    builder: B,
    fill: B.(Fields) -> Unit,
): T = fill(shape, builder, fill).build()

internal fun <T : Element, B : ElementBuilder<T>> Cursor.keyed(
    subject: String,
    builder: B,
    add: B.(Entry) -> Unit,
): T {
    builder.location = location
    if (!isMapping) report(DiagnosticCodes.FIELD_TYPE, "$subject must be an object")
    entries().forEach { entry -> builder.keyed(entry, add) }
    return builder.build()
}

internal fun <V> List<Entry>.mapped(read: (Cursor) -> V): Map<String, V> = associate { entry ->
    entry.key to read(entry.value)
}

internal fun <T : Element> List<Entry>.referables(
    kind: ReferenceKind,
    inline: (Cursor) -> T?,
): Map<String, Referable<T>> {
    val read = mapNotNull { entry -> Referables.read(entry.value, kind, inline)?.let { found -> entry.key to found } }
    return read.toMap()
}

private fun <B : ElementBuilder<*>> B.keyed(entry: Entry, add: B.(Entry) -> Unit) {
    val extension = ExtensionName.parseOrNull(entry.key)
    if (extension != null) return extension(extension, Values.of(entry.value))
    add(entry)
}
