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
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class ComponentUpgrade(private val context: UpgradeContext) {
    fun upgraded(root: YamlNode.Mapping): YamlNode? {
        val sections = Trees.entries(
            Keys.SCHEMAS to root[SwaggerKeys.DEFINITIONS]?.let(::schemasOf),
            Keys.PARAMETERS to parametersOf(root[Keys.PARAMETERS]),
            Keys.RESPONSES to root[Keys.RESPONSES]?.let(::responsesOf),
            Keys.SECURITY_SCHEMES to root[SwaggerKeys.SECURITY_DEFINITIONS]?.let(::securityOf),
        )
        return Trees.mappingOrNull(sections)
    }

    private fun schemasOf(definitions: YamlNode): YamlNode = Trees.mapped(definitions) { _, schema ->
        SchemaUpgrade.upgraded(schema)
    }

    private fun responsesOf(responses: YamlNode): YamlNode = Trees.mapped(responses) { name, response ->
        val place = Located(response, JsonPointer.of(Keys.RESPONSES, name))
        context.responses.upgraded(place, context.produces)
    }

    private fun securityOf(definitions: YamlNode): YamlNode = Trees.mapped(definitions) { _, scheme ->
        SecurityUpgrade.upgraded(scheme)
    }

    private fun parametersOf(node: YamlNode?): YamlNode? {
        val plain = Trees.mappingOrNull(Trees.entriesOf(node).filterNot { entry -> context.isBody(entry.value) })
        return plain?.let { shared ->
            Trees.mapped(shared) { name, parameter ->
                context.parameters.upgraded(Located(parameter, JsonPointer.of(Keys.PARAMETERS, name)))
            }
        }
    }
}
