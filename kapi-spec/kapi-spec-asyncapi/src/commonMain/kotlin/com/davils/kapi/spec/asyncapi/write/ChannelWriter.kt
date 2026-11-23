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
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.channel.Parameter
import com.davils.kapi.spec.asyncapi.read.shape.Keys

internal class ChannelWriter(
    private val info: InfoWriter,
    private val messages: MessageWriter,
    private val bindings: BindingWriter,
) {
    fun channel(channel: Channel): YamlNode {
        val out = Out()
        out.text(key = Keys.ADDRESS, value = channel.address)
        out.map(Keys.MESSAGES, channel.messages, ReferenceWriter.referables(messages::message))
        out.text(key = Keys.TITLE, value = channel.title)
        out.text(key = Keys.SUMMARY, value = channel.summary)
        out.text(key = Keys.DESCRIPTION, value = channel.description)
        out.list(Keys.SERVERS, channel.servers, ReferenceWriter::reference)
        out.map(Keys.PARAMETERS, channel.parameters, ReferenceWriter.referables(::parameter))
        info.described(out, channel.tags, channel.externalDocs)
        out.node(
            Keys.BINDINGS,
            channel.bindings?.let { written -> ReferenceWriter.referable(written, bindings::channel) },
        )
        return out.build(channel)
    }

    fun parameter(parameter: Parameter): YamlNode {
        val out = Out()
        out.texts(Keys.ENUM, parameter.enumeration)
        out.text(key = Keys.DEFAULT, value = parameter.default)
        out.text(key = Keys.DESCRIPTION, value = parameter.description)
        out.texts(Keys.EXAMPLES, parameter.examples)
        out.text(key = Keys.LOCATION, value = parameter.expression)
        return out.build(parameter)
    }
}
