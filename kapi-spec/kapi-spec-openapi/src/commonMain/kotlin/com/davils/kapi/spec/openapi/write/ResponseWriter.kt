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
import com.davils.kapi.spec.openapi.VersionFamily
import com.davils.kapi.spec.openapi.model.content.Link
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.content.Responses
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class ResponseWriter(
    private val content: ContentWriter,
    private val info: InfoWriter,
    family: VersionFamily,
) {
    private val isCurrent: Boolean = family.isAtLeast(VersionFamily.V32)

    fun responses(responses: Responses): YamlNode {
        val out = Out()
        responses.entries.forEach { (status, response) ->
            out.node(status.value, ReferenceWriter.referable(response, ::response))
        }
        return out.build(responses)
    }

    fun response(response: Response): YamlNode {
        val out = Out()
        out.text(Keys.DESCRIPTION, response.description)
        out.map(Keys.HEADERS, response.headers, ReferenceWriter.referables(content::header))
        out.map(Keys.CONTENT, response.content, ReferenceWriter.referables(content::mediaType))
        out.map(Keys.LINKS, response.links, ReferenceWriter.referables(::link))
        return out.build(response)
    }

    fun link(link: Link): YamlNode {
        val out = Out()
        out.text(Keys.OPERATION_REF, link.operationReference)
        out.text(Keys.OPERATION_ID, link.operationId)
        out.map(Keys.PARAMETERS, link.parameters, Nodes::of)
        out.node(Keys.REQUEST_BODY, link.requestBody?.let(Nodes::of))
        out.text(Keys.DESCRIPTION, link.description)
        out.node(Keys.SERVER, link.server?.let(info::server))
        return out.build(link)
    }
}
