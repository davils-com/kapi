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

import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.channel.ChannelBuilder
import com.davils.kapi.spec.asyncapi.model.channel.Parameter
import com.davils.kapi.spec.asyncapi.model.channel.ParameterBuilder
import com.davils.kapi.spec.asyncapi.read.reference.ReferenceKind
import com.davils.kapi.spec.asyncapi.read.reference.References
import com.davils.kapi.spec.asyncapi.read.shape.Keys
import com.davils.kapi.spec.asyncapi.read.shape.Shapes

internal object ChannelReader {
    private const val SERVER: String = "A server of a channel"

    fun channel(cursor: Cursor): Channel = cursor.read(Shapes.channel, ChannelBuilder()) { fields ->
        address = fields.text(Keys.ADDRESS)
        messages.putAll(fields.entries(Keys.MESSAGES).referables(ReferenceKind.Message, MessageReader::message))
        title = fields.text(Keys.TITLE)
        summary = fields.text(Keys.SUMMARY)
        description = fields.text(Keys.DESCRIPTION)
        val written = fields.items(Keys.SERVERS)
        servers.addAll(written.mapNotNull { item -> References.required(item, ReferenceKind.Server, SERVER) })
        parameters.putAll(fields.entries(Keys.PARAMETERS).referables(ReferenceKind.Parameter, ::parameter))
        tags.addAll(InfoReader.tagsOf(fields))
        externalDocs = InfoReader.docsOf(fields)
        bindings = fields.cursor(Keys.BINDINGS)?.referable(ReferenceKind.ChannelBindings, BindingReader::channel)
    }

    fun parameter(cursor: Cursor): Parameter = cursor.read(Shapes.parameter, ParameterBuilder()) { fields ->
        enumeration.addAll(fields.texts(Keys.ENUM))
        default = fields.text(Keys.DEFAULT)
        description = fields.text(Keys.DESCRIPTION)
        examples.addAll(fields.texts(Keys.EXAMPLES))
        expression = fields.text(Keys.LOCATION)
    }
}
