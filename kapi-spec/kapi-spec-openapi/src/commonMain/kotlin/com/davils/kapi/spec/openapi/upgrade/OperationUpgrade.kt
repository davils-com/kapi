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

private val replacedKeys: Set<String> = setOf(
    SwaggerKeys.CONSUMES,
    SwaggerKeys.PRODUCES,
    SwaggerKeys.SCHEMES,
    Keys.PARAMETERS,
    Keys.RESPONSES,
)

internal class OperationUpgrade(private val context: UpgradeContext) {
    fun upgraded(place: Located, inherited: List<Located>): YamlNode {
        val operation = place.node
        val declared = Located.items(operation[Keys.PARAMETERS], place.pointer.child(Keys.PARAMETERS))
        val (own, others) = declared.partition { parameter -> context.isBody(parameter.node) }
        val (contentTypes, plain) = others.partition(::isContentType)
        val consumes = typesOf(contentTypes) ?: Trees.texts(operation[SwaggerKeys.CONSUMES]) ?: context.consumes
        val produces = Trees.texts(operation[SwaggerKeys.PRODUCES]) ?: context.produces
        val parameters = Trees.sequenceOf(plain.map(context.parameters::upgraded)).takeIf { plain.isNotEmpty() }
        val responses = operation[Keys.RESPONSES]?.let { found ->
            responsesOf(Located(found, place.pointer.child(Keys.RESPONSES)), produces)
        }
        val schemes = Trees.texts(operation[SwaggerKeys.SCHEMES])
        val upgraded = Trees.entries(
            Keys.PARAMETERS to parameters,
            Keys.REQUEST_BODY to bodyOf(merged(own, inherited), consumes),
            Keys.RESPONSES to responses,
            Keys.SERVERS to schemes?.let(context.servers::ofOperation),
        )
        return Trees.mappingAt(operation, Trees.without(operation, replacedKeys) + upgraded)
    }

    private fun responsesOf(place: Located, produces: List<String>): YamlNode {
        val responses = Trees.mapped(place.node) { status, response ->
            context.responses.upgraded(Located(response, place.pointer.child(status)), produces)
        }
        return responses
    }

    private fun merged(own: List<Located>, inherited: List<Located>): List<Located> {
        val identities = own.map(::identityOf).toSet()
        return own + inherited.filterNot { parameter -> identityOf(parameter) in identities }
    }

    private fun identityOf(parameter: Located): String {
        val resolved = context.resolved(parameter.node)
        val placement = resolved[Keys.IN].string().orEmpty()
        if (placement == SwaggerKeys.BODY) return placement
        return "$placement:${resolved[Keys.NAME].string().orEmpty()}"
    }

    private fun bodyOf(parameters: List<Located>, consumes: List<String>): YamlNode? {
        val resolved = parameters.map { parameter -> Located(context.resolved(parameter.node), parameter.pointer) }
        val body = resolved.firstOrNull { parameter -> placementOf(parameter) == SwaggerKeys.BODY }
        val fields = resolved.filter { parameter -> placementOf(parameter) == SwaggerKeys.FORM_DATA }
        return body?.let { found -> context.bodies.fromBody(found.node, consumes) } ?: formOf(fields, consumes)
    }

    private fun formOf(fields: List<Located>, consumes: List<String>): YamlNode? {
        if (fields.isEmpty()) return null
        return context.bodies.fromForm(fields, consumes)
    }

    private fun placementOf(parameter: Located): String? = parameter.node[Keys.IN].string()

    private fun isContentType(parameter: Located): Boolean {
        val resolved = context.resolved(parameter.node)
        val isHeader = resolved[Keys.IN].string() == SwaggerKeys.HEADER
        return isHeader && resolved[Keys.NAME].string()?.lowercase() == SwaggerKeys.CONTENT_TYPE
    }

    private fun typesOf(contentTypes: List<Located>): List<String>? {
        val enumerations = contentTypes.map { parameter -> context.resolved(parameter.node)[Keys.ENUM] }
        val declared = enumerations.flatMap { values -> Trees.texts(values).orEmpty() }
        return declared.takeIf { types -> types.isNotEmpty() }
    }
}
