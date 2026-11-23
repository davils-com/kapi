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
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kapi.spec.openapi.read.shape.Keys

private val bodyPlacements: Set<String> = setOf(SwaggerKeys.BODY, SwaggerKeys.FORM_DATA)

internal class UpgradeContext(root: YamlNode.Mapping, notes: UpgradeNotes) {
    val consumes: List<String> = Trees.texts(root[SwaggerKeys.CONSUMES]) ?: listOf(SwaggerKeys.JSON)

    val produces: List<String> = Trees.texts(root[SwaggerKeys.PRODUCES]) ?: listOf(SwaggerKeys.JSON)

    val servers: Servers = Servers(
        host = root[SwaggerKeys.HOST].string(),
        basePath = root[SwaggerKeys.BASE_PATH].string(),
    )

    private val styles: CollectionStyles = CollectionStyles(notes)

    private val schemas: SimpleSchemaUpgrade = SimpleSchemaUpgrade(styles)

    val parameters: ParameterUpgrade = ParameterUpgrade(schemas, styles)

    val bodies: BodyUpgrade = BodyUpgrade(schemas, styles)

    val responses: ResponseUpgrade = ResponseUpgrade(schemas, styles)

    private val shared: Map<String, YamlNode> = sharedOf(root[Keys.PARAMETERS])

    fun resolved(parameter: YamlNode): YamlNode {
        val reference = ReferenceUpgrade.referenceOf(parameter)
        val name = reference?.let { found -> ReferenceUpgrade.localName(reference = found, section = Keys.PARAMETERS) }
        return name?.let(shared::get) ?: parameter
    }

    fun isBody(parameter: YamlNode): Boolean = resolved(parameter)[Keys.IN].string() in bodyPlacements

    private fun sharedOf(node: YamlNode?): Map<String, YamlNode> {
        val named = Trees.entriesOf(node).mapNotNull { entry ->
            Trees.keyOf(entry)?.let { name -> name to entry.value }
        }
        return named.toMap()
    }
}
