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

package com.davils.kapi.spec.asyncapi.read.reference

import com.davils.kapi.spec.asyncapi.read.BindingReader
import com.davils.kapi.spec.asyncapi.read.ChannelReader
import com.davils.kapi.spec.asyncapi.read.Cursor
import com.davils.kapi.spec.asyncapi.read.InfoReader
import com.davils.kapi.spec.asyncapi.read.MessageReader
import com.davils.kapi.spec.asyncapi.read.OperationReader
import com.davils.kapi.spec.asyncapi.read.SchemaReader
import com.davils.kapi.spec.asyncapi.read.SecurityReader
import com.davils.kapi.spec.asyncapi.read.ServerReader
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable

internal object TargetReaders {
    private val readers: Map<ReferenceKind, (Cursor) -> Element?> = mapOf(
        ReferenceKind.Schema to SchemaReader::schema,
        ReferenceKind.Server to ServerReader::server,
        ReferenceKind.ServerVariable to ServerReader::variable,
        ReferenceKind.Channel to ChannelReader::channel,
        ReferenceKind.Operation to OperationReader::operation,
        ReferenceKind.Message to MessageReader::message,
        ReferenceKind.SecurityScheme to SecurityReader::scheme,
        ReferenceKind.Parameter to ChannelReader::parameter,
        ReferenceKind.CorrelationId to MessageReader::correlationId,
        ReferenceKind.OperationReply to OperationReader::reply,
        ReferenceKind.ReplyAddress to OperationReader::replyAddress,
        ReferenceKind.ExternalDocs to InfoReader::externalDocs,
        ReferenceKind.Tag to InfoReader::tag,
        ReferenceKind.OperationTrait to OperationReader::trait,
        ReferenceKind.MessageTrait to MessageReader::trait,
        ReferenceKind.ServerBindings to BindingReader::server,
        ReferenceKind.ChannelBindings to BindingReader::channel,
        ReferenceKind.OperationBindings to BindingReader::operation,
        ReferenceKind.MessageBindings to BindingReader::message,
    )

    fun read(kind: ReferenceKind, cursor: Cursor): Referable<Element>? =
        Referables.read(cursor, kind, readers.getValue(kind))
}
