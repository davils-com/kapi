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

package com.davils.kapi.spec.asyncapi.write

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.asyncapi.model.info.Contact
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Info
import com.davils.kapi.spec.asyncapi.model.info.License
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.asyncapi.model.server.ServerVariable
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.core.model.Referable

internal class InfoWriter(private val security: SecurityWriter, private val bindings: BindingWriter) {
    fun info(info: Info): YamlNode {
        val out = Out()
        out.text(key = Keys.TITLE, value = info.title)
        out.text(key = Keys.VERSION, value = info.version)
        out.text(key = Keys.DESCRIPTION, value = info.description)
        out.text(key = Keys.TERMS_OF_SERVICE, value = info.termsOfService)
        out.node(Keys.CONTACT, info.contact?.let(::contact))
        out.node(Keys.LICENSE, info.license?.let(::license))
        described(out, info.tags, info.externalDocs)
        return out.build(info)
    }

    fun described(out: Out, tags: List<Referable<Tag>>, docs: Referable<ExternalDocs>?) {
        out.list(Keys.TAGS, tags, ReferenceWriter.referables(::tag))
        out.node(Keys.EXTERNAL_DOCS, docs?.let { written -> ReferenceWriter.referable(written, ::externalDocs) })
    }

    fun externalDocs(docs: ExternalDocs): YamlNode {
        val out = Out()
        out.text(key = Keys.DESCRIPTION, value = docs.description)
        out.text(key = Keys.URL, value = docs.url)
        return out.build(docs)
    }

    fun tag(tag: Tag): YamlNode {
        val out = Out()
        out.text(key = Keys.NAME, value = tag.name)
        out.text(key = Keys.DESCRIPTION, value = tag.description)
        out.node(Keys.EXTERNAL_DOCS, tag.externalDocs?.let { docs -> ReferenceWriter.referable(docs, ::externalDocs) })
        return out.build(tag)
    }

    fun server(server: Server): YamlNode {
        val out = Out()
        out.text(key = Keys.HOST, value = server.host)
        out.text(key = Keys.PROTOCOL, value = server.protocol)
        out.text(key = Keys.PROTOCOL_VERSION, value = server.protocolVersion)
        out.text(key = Keys.PATHNAME, value = server.pathname)
        out.text(key = Keys.DESCRIPTION, value = server.description)
        out.text(key = Keys.TITLE, value = server.title)
        out.text(key = Keys.SUMMARY, value = server.summary)
        out.map(Keys.VARIABLES, server.variables, ReferenceWriter.referables(::variable))
        out.list(Keys.SECURITY, server.security, ReferenceWriter.referables(security::scheme))
        described(out, server.tags, server.externalDocs)
        out.node(
            Keys.BINDINGS,
            server.bindings?.let { written -> ReferenceWriter.referable(written, bindings::server) },
        )
        return out.build(server)
    }

    fun variable(variable: ServerVariable): YamlNode {
        val out = Out()
        out.texts(Keys.ENUM, variable.enumeration)
        out.text(key = Keys.DEFAULT, value = variable.default)
        out.text(key = Keys.DESCRIPTION, value = variable.description)
        out.texts(Keys.EXAMPLES, variable.examples)
        return out.build(variable)
    }

    private fun contact(contact: Contact): YamlNode {
        val out = Out()
        out.text(key = Keys.NAME, value = contact.name)
        out.text(key = Keys.URL, value = contact.url)
        out.text(key = Keys.EMAIL, value = contact.email)
        return out.build(contact)
    }

    private fun license(license: License): YamlNode {
        val out = Out()
        out.text(key = Keys.NAME, value = license.name)
        out.text(key = Keys.URL, value = license.url)
        return out.build(license)
    }
}
