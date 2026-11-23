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

package com.davils.kapi.spec.openapi.read

import com.davils.kapi.spec.openapi.DocumentBuilder
import com.davils.kapi.spec.openapi.Version
import com.davils.kapi.spec.openapi.read.component.ComponentsReader
import com.davils.kapi.spec.openapi.read.info.InfoReader
import com.davils.kapi.spec.openapi.read.path.PathReader
import com.davils.kapi.spec.openapi.read.security.SecurityReader
import com.davils.kapi.spec.openapi.read.server.ServerReader
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kapi.spec.openapi.read.shape.Shapes

internal object DocumentReader {
    fun fill(cursor: Cursor, version: Version): DocumentBuilder =
        cursor.fill(Shapes.document, DocumentBuilder()) { fields ->
            this.version = version
            fields.cursor(Keys.INFO)?.let { declared -> info = InfoReader.info(declared) }
            jsonSchemaDialect = fields.text(Keys.JSON_SCHEMA_DIALECT)
            servers.addAll(fields.items(Keys.SERVERS).map(ServerReader::server))
            paths = fields.cursor(Keys.PATHS)?.let(PathReader::paths)
            webhooks.putAll(fields.entries(Keys.WEBHOOKS).mapped(PathReader::pathItem))
            components = fields.cursor(Keys.COMPONENTS)?.let(ComponentsReader::components)
            security.addAll(fields.items(Keys.SECURITY).map(SecurityReader::requirement))
            tags.addAll(fields.items(Keys.TAGS).map(InfoReader::tag))
            externalDocs = fields.cursor(Keys.EXTERNAL_DOCS)?.let(InfoReader::externalDocs)
        }
}
