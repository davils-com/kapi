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

package com.davils.kapi.spec.asyncapi.read.shape

internal object Shapes {
    private const val OPERATIONS_HINT: String = "AsyncAPI 3 describes what an application does in operations"

    private const val KEY_HINT: String = "the key of the object is its id"

    private val described: List<FieldRule> = listOf(
        FieldRule(Keys.TITLE, FieldKind.Text),
        FieldRule(Keys.SUMMARY, FieldKind.Text),
        FieldRule(Keys.DESCRIPTION, FieldKind.Text),
    )

    private val tagged: List<FieldRule> = listOf(
        FieldRule(Keys.TAGS, FieldKind.Sequence),
        FieldRule(Keys.EXTERNAL_DOCS, FieldKind.Mapping),
        FieldRule(Keys.BINDINGS, FieldKind.Mapping),
    )

    private val operationTraitFields: List<FieldRule> = described +
        tagged +
        FieldRule(
            Keys.SECURITY,
            FieldKind.Sequence,
        )

    private val messageTraitFields: List<FieldRule> = described +
        tagged +
        listOf(
            FieldRule(Keys.HEADERS, FieldKind.Schema),
            FieldRule(Keys.CORRELATION_ID, FieldKind.Mapping),
            FieldRule(Keys.CONTENT_TYPE, FieldKind.Text),
            FieldRule(Keys.NAME, FieldKind.Text),
            FieldRule(Keys.EXAMPLES, FieldKind.Sequence),
        )

    private val messageRetired: Map<String, String> = mapOf(
        Keys.MESSAGE_ID to KEY_HINT,
        Keys.SCHEMA_FORMAT to "write the payload as a multi-format schema with schemaFormat and schema",
    )

    val document: ObjectShape = ObjectShape(
        subject = "The document",
        fields = listOf(
            FieldRule(Keys.ASYNCAPI, FieldKind.Value, isRequired = true),
            FieldRule(Keys.ID, FieldKind.Text),
            FieldRule(Keys.INFO, FieldKind.Mapping, isRequired = true),
            FieldRule(Keys.SERVERS, FieldKind.Mapping),
            FieldRule(Keys.DEFAULT_CONTENT_TYPE, FieldKind.Text),
            FieldRule(Keys.CHANNELS, FieldKind.Mapping),
            FieldRule(Keys.OPERATIONS, FieldKind.Mapping),
            FieldRule(Keys.COMPONENTS, FieldKind.Mapping),
        ),
        retired = mapOf(
            Keys.TAGS to "the tags of the application are part of info",
            Keys.EXTERNAL_DOCS to "the external documentation of the application is part of info",
        ),
    )

    val info: ObjectShape = ObjectShape(
        subject = "The info",
        fields = listOf(
            FieldRule(Keys.TITLE, FieldKind.Text, isRequired = true),
            FieldRule(Keys.VERSION, FieldKind.Text, isRequired = true),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.TERMS_OF_SERVICE, FieldKind.Text),
            FieldRule(Keys.CONTACT, FieldKind.Mapping),
            FieldRule(Keys.LICENSE, FieldKind.Mapping),
            FieldRule(Keys.TAGS, FieldKind.Sequence),
            FieldRule(Keys.EXTERNAL_DOCS, FieldKind.Mapping),
        ),
    )

    val contact: ObjectShape = ObjectShape(
        subject = "A contact",
        fields = listOf(
            FieldRule(Keys.NAME, FieldKind.Text),
            FieldRule(Keys.URL, FieldKind.Text),
            FieldRule(Keys.EMAIL, FieldKind.Text),
        ),
    )

    val license: ObjectShape = ObjectShape(
        subject = "A license",
        fields = listOf(
            FieldRule(Keys.NAME, FieldKind.Text, isRequired = true),
            FieldRule(Keys.URL, FieldKind.Text),
        ),
    )

    val tag: ObjectShape = ObjectShape(
        subject = "A tag",
        fields = listOf(
            FieldRule(Keys.NAME, FieldKind.Text, isRequired = true),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.EXTERNAL_DOCS, FieldKind.Mapping),
        ),
    )

    val externalDocs: ObjectShape = ObjectShape(
        subject = "External documentation",
        fields = listOf(
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.URL, FieldKind.Text, isRequired = true),
        ),
    )

    val server: ObjectShape = ObjectShape(
        subject = "A server",
        fields = described +
            tagged +
            listOf(
                FieldRule(Keys.HOST, FieldKind.Text, isRequired = true),
                FieldRule(Keys.PROTOCOL, FieldKind.Text, isRequired = true),
                FieldRule(Keys.PROTOCOL_VERSION, FieldKind.Text),
                FieldRule(Keys.PATHNAME, FieldKind.Text),
                FieldRule(Keys.VARIABLES, FieldKind.Mapping),
                FieldRule(Keys.SECURITY, FieldKind.Sequence),
            ),
        retired = mapOf(Keys.URL to "a server names its host and its pathname instead"),
    )

    val serverVariable: ObjectShape = ObjectShape(
        subject = "A server variable",
        fields = listOf(
            FieldRule(Keys.ENUM, FieldKind.Sequence),
            FieldRule(Keys.DEFAULT, FieldKind.Text),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.EXAMPLES, FieldKind.Sequence),
        ),
    )

    val channel: ObjectShape = ObjectShape(
        subject = "A channel",
        fields = described +
            tagged +
            listOf(
                FieldRule(Keys.ADDRESS, FieldKind.TextOrNull),
                FieldRule(Keys.MESSAGES, FieldKind.Mapping),
                FieldRule(Keys.SERVERS, FieldKind.Sequence),
                FieldRule(Keys.PARAMETERS, FieldKind.Mapping),
            ),
        retired = mapOf(Keys.PUBLISH to OPERATIONS_HINT, Keys.SUBSCRIBE to OPERATIONS_HINT),
    )

    val parameter: ObjectShape = ObjectShape(
        subject = "A parameter",
        fields = listOf(
            FieldRule(Keys.ENUM, FieldKind.Sequence),
            FieldRule(Keys.DEFAULT, FieldKind.Text),
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.EXAMPLES, FieldKind.Sequence),
            FieldRule(Keys.LOCATION, FieldKind.Text),
        ),
        retired = mapOf(Keys.SCHEMA to "a parameter is a string described by enum, default and examples"),
    )

    val operation: ObjectShape = ObjectShape(
        subject = "An operation",
        fields = operationTraitFields +
            listOf(
                FieldRule(Keys.ACTION, FieldKind.Text, isRequired = true),
                FieldRule(Keys.CHANNEL, FieldKind.Mapping, isRequired = true),
                FieldRule(Keys.TRAITS, FieldKind.Sequence),
                FieldRule(Keys.MESSAGES, FieldKind.Sequence),
                FieldRule(Keys.REPLY, FieldKind.Mapping),
            ),
        retired = mapOf(Keys.OPERATION_ID to KEY_HINT, Keys.MESSAGE to "an operation lists its messages"),
    )

    val operationTrait: ObjectShape = ObjectShape(
        subject = "An operation trait",
        fields = operationTraitFields,
        retired = mapOf(Keys.OPERATION_ID to KEY_HINT),
    )

    val reply: ObjectShape = ObjectShape(
        subject = "A reply",
        fields = listOf(
            FieldRule(Keys.ADDRESS, FieldKind.Mapping),
            FieldRule(Keys.CHANNEL, FieldKind.Mapping),
            FieldRule(Keys.MESSAGES, FieldKind.Sequence),
        ),
    )

    val replyAddress: ObjectShape = ObjectShape(
        subject = "A reply address",
        fields = listOf(
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.LOCATION, FieldKind.Text, isRequired = true),
        ),
    )

    val message: ObjectShape = ObjectShape(
        subject = "A message",
        fields = messageTraitFields +
            listOf(
                FieldRule(Keys.PAYLOAD, FieldKind.Schema),
                FieldRule(Keys.TRAITS, FieldKind.Sequence),
            ),
        retired = messageRetired,
    )

    val messageTrait: ObjectShape = ObjectShape(
        subject = "A message trait",
        fields = messageTraitFields,
        retired = messageRetired,
    )

    val messageExample: ObjectShape = ObjectShape(
        subject = "A message example",
        fields = listOf(
            FieldRule(Keys.HEADERS, FieldKind.Mapping),
            FieldRule(Keys.PAYLOAD, FieldKind.Value),
            FieldRule(Keys.NAME, FieldKind.Text),
            FieldRule(Keys.SUMMARY, FieldKind.Text),
        ),
    )

    val correlationId: ObjectShape = ObjectShape(
        subject = "A correlation id",
        fields = listOf(
            FieldRule(Keys.DESCRIPTION, FieldKind.Text),
            FieldRule(Keys.LOCATION, FieldKind.Text, isRequired = true),
        ),
    )

    val multiFormatSchema: ObjectShape = ObjectShape(
        subject = "A multi-format schema",
        fields = listOf(
            FieldRule(Keys.SCHEMA_FORMAT, FieldKind.Text, isRequired = true),
            FieldRule(Keys.SCHEMA, FieldKind.Value, isRequired = true),
        ),
    )

    val components: ObjectShape = ObjectShape(
        subject = "The components",
        fields = listOf(
            Keys.SCHEMAS,
            Keys.SERVERS,
            Keys.CHANNELS,
            Keys.OPERATIONS,
            Keys.MESSAGES,
            Keys.SECURITY_SCHEMES,
            Keys.SERVER_VARIABLES,
            Keys.PARAMETERS,
            Keys.CORRELATION_IDS,
            Keys.REPLIES,
            Keys.REPLY_ADDRESSES,
            Keys.EXTERNAL_DOCS,
            Keys.TAGS,
            Keys.OPERATION_TRAITS,
            Keys.MESSAGE_TRAITS,
            Keys.SERVER_BINDINGS,
            Keys.CHANNEL_BINDINGS,
            Keys.OPERATION_BINDINGS,
            Keys.MESSAGE_BINDINGS,
        ).map { name -> FieldRule(name, FieldKind.Mapping) },
    )

    val bindings: ObjectShape = ObjectShape(
        subject = "The bindings",
        fields = Keys.protocols.map { protocol -> FieldRule(protocol, FieldKind.Mapping) },
    )

    val webSocketChannel: ObjectShape = ObjectShape(
        subject = "The WebSocket binding of a channel",
        fields = listOf(
            FieldRule(Keys.METHOD, FieldKind.Text),
            FieldRule(Keys.QUERY, FieldKind.Schema),
            FieldRule(Keys.HEADERS, FieldKind.Schema),
            FieldRule(Keys.BINDING_VERSION, FieldKind.Text),
        ),
    )

    val webSocketReserved: ObjectShape = ObjectShape(subject = "This WebSocket binding", fields = emptyList())
}
