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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class ParameterUpgrade(
    private val schemas: SimpleSchemaUpgrade,
    private val styles: CollectionStyles,
) {
    fun upgraded(place: Located): YamlNode {
        if (ReferenceUpgrade.referenceOf(place.node) != null) return ReferenceUpgrade.upgradedNode(place.node)
        val kept = Trees.without(place.node, SimpleSchemaUpgrade.keys + SwaggerKeys.COLLECTION_FORMAT)
        val schema = Trees.entries(Keys.SCHEMA to schemas.schemaOf(place))
        return Trees.mappingAt(place.node, kept + schema + styles.styleOf(place))
    }
}
