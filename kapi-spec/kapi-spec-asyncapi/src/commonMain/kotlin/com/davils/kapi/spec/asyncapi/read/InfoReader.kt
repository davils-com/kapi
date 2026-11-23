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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.model.info.Contact
import com.davils.kapi.spec.asyncapi.model.info.ContactBuilder
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocsBuilder
import com.davils.kapi.spec.asyncapi.model.info.Info
import com.davils.kapi.spec.asyncapi.model.info.InfoBuilder
import com.davils.kapi.spec.asyncapi.model.info.License
import com.davils.kapi.spec.asyncapi.model.info.LicenseBuilder
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.info.TagBuilder
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.shape.Fields
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.Shapes
import com.davils.kapi.spec.core.model.Referable

internal object InfoReader {
    fun info(cursor: Cursor): Info = cursor.read(Shapes.info, InfoBuilder()) { fields ->
        title = fields.text(Keys.TITLE).orEmpty()
        version = fields.text(Keys.VERSION).orEmpty()
        description = fields.text(Keys.DESCRIPTION)
        termsOfService = fields.text(Keys.TERMS_OF_SERVICE)
        contact = fields.cursor(Keys.CONTACT)?.let(::contact)
        license = fields.cursor(Keys.LICENSE)?.let(::license)
        tags.addAll(tagsOf(fields))
        externalDocs = docsOf(fields)
    }

    fun tagsOf(fields: Fields): List<Referable<Tag>> = fields.items(Keys.TAGS).referables(ReferenceKind.Tag, ::tag)

    fun docsOf(fields: Fields): Referable<ExternalDocs>? =
        fields.cursor(Keys.EXTERNAL_DOCS)?.referable(ReferenceKind.ExternalDocs, ::externalDocs)

    fun tag(cursor: Cursor): Tag = cursor.read(Shapes.tag, TagBuilder()) { fields ->
        name = fields.text(Keys.NAME).orEmpty()
        description = fields.text(Keys.DESCRIPTION)
        externalDocs = docsOf(fields)
    }

    fun externalDocs(cursor: Cursor): ExternalDocs = cursor.read(Shapes.externalDocs, ExternalDocsBuilder()) { fields ->
        url = fields.text(Keys.URL).orEmpty()
        description = fields.text(Keys.DESCRIPTION)
    }

    private fun contact(cursor: Cursor): Contact = cursor.read(Shapes.contact, ContactBuilder()) { fields ->
        name = fields.text(Keys.NAME)
        url = fields.text(Keys.URL)
        email = fields.text(Keys.EMAIL)
    }

    private fun license(cursor: Cursor): License = cursor.read(Shapes.license, LicenseBuilder()) { fields ->
        name = fields.text(Keys.NAME).orEmpty()
        url = fields.text(Keys.URL)
    }
}
