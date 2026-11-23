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
import com.davils.kapi.spec.openapi.model.info.Contact
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kapi.spec.openapi.model.info.Info
import com.davils.kapi.spec.openapi.model.info.License
import com.davils.kapi.spec.openapi.model.info.Tag
import com.davils.kapi.spec.openapi.model.server.Server
import com.davils.kapi.spec.openapi.model.server.ServerVariable
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class InfoWriter(family: VersionFamily) {
    private val isCurrent: Boolean = family.isAtLeast(VersionFamily.V32)

    fun info(info: Info): YamlNode {
        val out = Out()
        out.text(Keys.TITLE, info.title)
        out.text(Keys.VERSION, info.version)
        out.text(Keys.SUMMARY, info.summary)
        out.text(Keys.DESCRIPTION, info.description)
        out.text(Keys.TERMS_OF_SERVICE, info.termsOfService)
        out.node(Keys.CONTACT, info.contact?.let(::contact))
        out.node(Keys.LICENSE, info.license?.let(::license))
        return out.build(info)
    }

    fun externalDocs(docs: ExternalDocs): YamlNode {
        val out = Out()
        out.text(Keys.URL, docs.url)
        out.text(Keys.DESCRIPTION, docs.description)
        return out.build(docs)
    }

    fun tag(tag: Tag): YamlNode {
        val out = Out()
        out.text(Keys.NAME, tag.name)
        out.text(Keys.SUMMARY, tag.summary?.takeIf { isCurrent })
        out.text(Keys.DESCRIPTION, tag.description)
        out.node(Keys.EXTERNAL_DOCS, tag.externalDocs?.let(::externalDocs))
        out.text(Keys.PARENT, tag.parent?.takeIf { isCurrent })
        out.text(Keys.KIND, tag.kind?.takeIf { isCurrent })
        return out.build(tag)
    }

    fun server(server: Server): YamlNode {
        val out = Out()
        out.text(Keys.URL, server.url)
        out.text(Keys.DESCRIPTION, server.description)
        out.map(Keys.VARIABLES, server.variables, ::variable)
        return out.build(server)
    }

    private fun variable(variable: ServerVariable): YamlNode {
        val out = Out()
        out.texts(Keys.ENUM, variable.enumeration)
        out.text(Keys.DEFAULT, variable.default)
        out.text(Keys.DESCRIPTION, variable.description)
        return out.build(variable)
    }

    private fun contact(contact: Contact): YamlNode {
        val out = Out()
        out.text(Keys.NAME, contact.name)
        out.text(Keys.URL, contact.url)
        out.text(Keys.EMAIL, contact.email)
        return out.build(contact)
    }

    private fun license(license: License): YamlNode {
        val out = Out()
        out.text(Keys.NAME, license.name)
        out.text(Keys.IDENTIFIER, license.identifier)
        out.text(Keys.URL, license.url)
        return out.build(license)
    }
}
