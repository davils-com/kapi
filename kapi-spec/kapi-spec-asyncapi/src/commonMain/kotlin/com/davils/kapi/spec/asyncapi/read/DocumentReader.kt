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

import com.davils.kapi.spec.asyncapi.DocumentBuilder
import com.davils.kapi.spec.asyncapi.Version
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.Shapes

internal object DocumentReader {
    fun fill(cursor: Cursor, version: Version): DocumentBuilder =
        cursor.fill(Shapes.document, DocumentBuilder()) { fields ->
            this.version = version
            id = fields.text(Keys.ID)
            fields.cursor(Keys.INFO)?.let { declared -> info = InfoReader.info(declared) }
            servers.putAll(fields.entries(Keys.SERVERS).referables(ReferenceKind.Server, ServerReader::server))
            defaultContentType = fields.text(Keys.DEFAULT_CONTENT_TYPE)
            channels.putAll(fields.entries(Keys.CHANNELS).referables(ReferenceKind.Channel, ChannelReader::channel))
            val operations = fields.entries(Keys.OPERATIONS)
            this.operations.putAll(operations.referables(ReferenceKind.Operation, OperationReader::operation))
            components = fields.cursor(Keys.COMPONENTS)?.let(ComponentsReader::components)
        }
}
