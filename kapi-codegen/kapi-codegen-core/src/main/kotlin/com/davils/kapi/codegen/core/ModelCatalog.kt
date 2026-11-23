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
 * The models one generation emitted, so that a generation of another document that references them uses them
 * instead of generating them again.
 *
 * @property document the name of the document the models come from.
 * @property packageName the package of the models.
 * @property models every model by the JSON Pointer of its schema.
 * @since 1.0.0
 */
@Serializable
public class ModelCatalog(
    public val document: String,
    public val packageName: String,
    public val models: Map<String, CatalogModel>,
) {
    override fun equals(other: Any?): Boolean = other is ModelCatalog &&
        document == other.document &&
        packageName == other.packageName &&
        models == other.models

    override fun hashCode(): Int = listOf(document, packageName, models).hashCode()

    override fun toString(): String = "ModelCatalog($document, $packageName, ${models.size} models)"
}
