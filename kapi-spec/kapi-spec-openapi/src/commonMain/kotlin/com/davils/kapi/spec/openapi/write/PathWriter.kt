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

package com.davils.kapi.spec.openapi.write

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlSequence
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.path.Callback
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.path.Paths
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class PathWriter(
    private val content: ContentWriter,
    private val responses: ResponseWriter,
    private val info: InfoWriter,
    private val security: SecurityWriter,
    family: VersionFamily,
) {
    private val isCurrent: Boolean = family.isAtLeast(VersionFamily.V32)

    fun paths(paths: Paths): YamlNode {
        val out = Out()
        paths.items.forEach { (template, item) -> out.node(template.value, pathItem(item)) }
        return out.build(paths)
    }

    fun pathItem(item: PathItem): YamlNode {
        val out = Out()
        out.text(Keys.REFERENCE, item.reference?.text)
        out.text(Keys.SUMMARY, item.summary)
        out.text(Keys.DESCRIPTION, item.description)
        val methods = item.operations.filterKeys { method -> method != Method.Query || isCurrent }
        methods.forEach { (method, operation) -> out.node(method.keyword, operation(operation)) }
        val additional = item.additionalOperations.takeIf { isCurrent }.orEmpty()
        out.map(Keys.ADDITIONAL_OPERATIONS, additional, ::operation)
        out.list(Keys.SERVERS, item.servers, info::server)
        out.list(Keys.PARAMETERS, item.parameters, ReferenceWriter.referables(content::parameter))
        return out.build(item)
    }

    fun callback(callback: Callback): YamlNode {
        val out = Out()
        callback.expressions.forEach { (expression, item) -> out.node(expression, pathItem(item)) }
        return out.build(callback)
    }

    private fun operation(operation: Operation): YamlNode {
        val out = Out()
        out.texts(Keys.TAGS, operation.tags)
        out.text(Keys.SUMMARY, operation.summary)
        out.text(Keys.DESCRIPTION, operation.description)
        out.node(Keys.EXTERNAL_DOCS, operation.externalDocs?.let(info::externalDocs))
        out.text(Keys.OPERATION_ID, operation.operationId)
        out.list(Keys.PARAMETERS, operation.parameters, ReferenceWriter.referables(content::parameter))
        exchange(out, operation)
        out.flag(Keys.DEPRECATED, operation.isDeprecated)
        val requirements = operation.security?.map(security::requirement)
        out.node(Keys.SECURITY, requirements?.let { written -> yamlSequence(written) })
        out.list(Keys.SERVERS, operation.servers, info::server)
        return out.build(operation)
    }

    private fun exchange(out: Out, operation: Operation) {
        val body = operation.requestBody
        out.node(Keys.REQUEST_BODY, body?.let { declared -> ReferenceWriter.referable(declared, content::requestBody) })
        val declared = operation.responses
        val isDeclared = declared.entries.isNotEmpty() || !declared.extensions.isEmpty
        out.node(Keys.RESPONSES, responses.responses(declared).takeIf { isDeclared })
        out.map(Keys.CALLBACKS, operation.callbacks, ReferenceWriter.referables(::callback))
    }
}
