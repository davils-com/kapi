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

import com.davils.kapi.spec.asyncapi.read.reference.Referables
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.shape.Fields
import com.davils.kapi.spec.asyncapi.read.shape.ObjectShape
import com.davils.kapi.spec.asyncapi.read.shape.ShapeCheck
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
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

internal fun <T : Element> Cursor.referable(kind: ReferenceKind, inline: (Cursor) -> T?): Referable<T>? =
    Referables.read(this, kind, inline)

internal fun <T : Element> List<Entry>.referables(
    kind: ReferenceKind,
    inline: (Cursor) -> T?,
): Map<String, Referable<T>> {
    val read = mapNotNull { entry -> entry.value.referable(kind, inline)?.let { found -> entry.key to found } }
    return read.toMap()
}

internal fun <T : Element> List<Cursor>.referables(kind: ReferenceKind, inline: (Cursor) -> T?): List<Referable<T>> =
    mapNotNull { item -> item.referable(kind, inline) }
