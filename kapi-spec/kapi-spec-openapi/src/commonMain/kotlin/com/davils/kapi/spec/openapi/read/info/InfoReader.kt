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

package com.davils.kapi.spec.openapi.read.info

import com.davils.kapi.spec.openapi.model.info.Contact
import com.davils.kapi.spec.openapi.model.info.ContactBuilder
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kapi.spec.openapi.model.info.ExternalDocsBuilder
import com.davils.kapi.spec.openapi.model.info.Info
import com.davils.kapi.spec.openapi.model.info.InfoBuilder
import com.davils.kapi.spec.openapi.model.info.License
import com.davils.kapi.spec.openapi.model.info.LicenseBuilder
import com.davils.kapi.spec.openapi.model.info.Tag
import com.davils.kapi.spec.openapi.model.info.TagBuilder
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.read
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object InfoReader {
    fun info(cursor: Cursor): Info = cursor.read(Shapes.info, InfoBuilder()) { fields ->
        title = fields.text(Keys.TITLE).orEmpty()
        version = fields.text(Keys.VERSION).orEmpty()
        summary = fields.text(Keys.SUMMARY)
        description = fields.text(Keys.DESCRIPTION)
        termsOfService = fields.text(Keys.TERMS_OF_SERVICE)
        contact = fields.cursor(Keys.CONTACT)?.let(::contact)
        license = fields.cursor(Keys.LICENSE)?.let(::license)
    }

    fun externalDocs(cursor: Cursor): ExternalDocs = cursor.read(Shapes.externalDocs, ExternalDocsBuilder()) { fields ->
        url = fields.text(Keys.URL).orEmpty()
        description = fields.text(Keys.DESCRIPTION)
    }

    fun tag(cursor: Cursor): Tag = cursor.read(Shapes.tag, TagBuilder()) { fields ->
        name = fields.text(Keys.NAME).orEmpty()
        description = fields.text(Keys.DESCRIPTION)
        externalDocs = fields.cursor(Keys.EXTERNAL_DOCS)?.let(::externalDocs)
    }

    private fun contact(cursor: Cursor): Contact = cursor.read(Shapes.contact, ContactBuilder()) { fields ->
        name = fields.text(Keys.NAME)
        url = fields.text(Keys.URL)
        email = fields.text(Keys.EMAIL)
    }

    private fun license(cursor: Cursor): License = cursor.read(Shapes.license, LicenseBuilder()) { fields ->
        name = fields.text(Keys.NAME).orEmpty()
        identifier = fields.text(Keys.IDENTIFIER)
        url = fields.text(Keys.URL)
    }
}
