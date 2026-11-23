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

package com.davils.kapi.codegen.core

import kotlinx.serialization.Serializable

/**
 * A model a generation emitted, as another generation finds it in a [ModelCatalog].
 *
 * @property type the simple name of the generated type, such as `PetstorePet`.
 * @property kind whether the type is an object, an enumeration or a union.
 * @property hasWriteShape whether the type has a shape of its own to write, named after it with `Write`.
 * @since 1.0.0
 */
@Serializable
public class CatalogModel(
    public val type: String,
    public val kind: ModelKind,
    public val hasWriteShape: Boolean,
) {
    override fun equals(other: Any?): Boolean =
        other is CatalogModel && type == other.type && kind == other.kind && hasWriteShape == other.hasWriteShape

    override fun hashCode(): Int = listOf(type, kind, hasWriteShape).hashCode()

    override fun toString(): String = "CatalogModel($type, $kind, hasWriteShape=$hasWriteShape)"
}
