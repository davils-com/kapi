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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the diagnostics and option violations of the AsyncAPI generator, each `codegen.asyncapi.<name>`.
 *
 * The codes are stable API: a code keeps its meaning across versions. Unless a code says otherwise, a diagnostic with
 * it is an error, which refuses the whole document.
 *
 * @since 1.0.0
 */
public object GenerationCodes {
    /**
     * A warning: the channel is served over another protocol than WebSocket, so the generator skips it.
     *
     * @since 1.0.0
     */
    public val PROTOCOL_UNSUPPORTED: ViolationCode = code("protocol.unsupported")

    /**
     * A warning: the channel has no address, or no operation names it, so the generator skips it.
     *
     * @since 1.0.0
     */
    public val CHANNEL_SKIPPED: ViolationCode = code("channel.skipped")

    /**
     * The address of the channel is no path template the generator can serve, such as one with a query.
     *
     * @since 1.0.0
     */
    public val CHANNEL_ADDRESS: ViolationCode = code("channel.address")

    /**
     * The `ws` binding of the channel opens it with `POST`, which a WebSocket handshake of RFC 6455 cannot.
     *
     * @since 1.0.0
     */
    public val BINDING_METHOD: ViolationCode = code("binding.method")

    /**
     * The `query` or `headers` of the `ws` binding is no object of scalar properties.
     *
     * @since 1.0.0
     */
    public val HANDSHAKE_UNSUPPORTED: ViolationCode = code("handshake.unsupported")

    /**
     * A warning: the `headers` of the `ws` binding name a field the WebSocket handshake writes itself.
     *
     * @since 1.0.0
     */
    public val HANDSHAKE_RESERVED: ViolationCode = code("handshake.reserved")

    /**
     * A schema of a message is written in a format the generator has no Kotlin for, such as Avro, RAML or Protobuf.
     *
     * @since 1.0.0
     */
    public val SCHEMA_FORMAT: ViolationCode = code("schema.format")

    /**
     * A schema uses a keyword of JSON Schema draft-07 the generator cannot express, such as `items` as a list.
     *
     * @since 1.0.0
     */
    public val SCHEMA_UNSUPPORTED: ViolationCode = code("schema.unsupported")

    /**
     * A warning: keywords stand beside a `$ref`, which JSON Schema draft-07 ignores, and so does the generator.
     *
     * @since 1.0.0
     */
    public val SCHEMA_REFERENCE_SIBLING: ViolationCode = code("schema.reference-sibling")

    /**
     * A `$ref` inside a schema names nothing the reader resolved to a schema.
     *
     * @since 1.0.0
     */
    public val REFERENCE_UNSUPPORTED: ViolationCode = code("reference.unsupported")

    /**
     * The content type of a message is no JSON, or the messages of one channel have different content types.
     *
     * @since 1.0.0
     */
    public val CONTENT_TYPE_UNSUPPORTED: ViolationCode = code("content-type.unsupported")

    /**
     * A warning: neither the message nor the document names a content type, so the messages are read and written as
     * `application/json`.
     *
     * @since 1.0.0
     */
    public val CONTENT_TYPE_ASSUMED: ViolationCode = code("content-type.assumed")

    /**
     * Several messages travel in one direction of a channel, but no required property with a constant value tells
     * them apart.
     *
     * @since 1.0.0
     */
    public val MESSAGE_INDISTINCT: ViolationCode = code("message.indistinct")

    /**
     * A warning: a message declares headers, which the frames of a WebSocket do not carry; the headers are neither
     * sent nor received.
     *
     * @since 1.0.0
     */
    public val MESSAGE_HEADERS: ViolationCode = code("message.headers")

    /**
     * An operation names a message that is none of the messages of its channel.
     *
     * @since 1.0.0
     */
    public val MESSAGE_FOREIGN: ViolationCode = code("message.foreign")

    /**
     * The payload of a message is no model, no scalar and no list of either.
     *
     * @since 1.0.0
     */
    public val PAYLOAD_UNSUPPORTED: ViolationCode = code("payload.unsupported")

    /**
     * An operation names no channel of the document.
     *
     * @since 1.0.0
     */
    public val OPERATION_CHANNEL: ViolationCode = code("operation.channel")

    /**
     * The reply of an operation goes to another channel or to a dynamic address, which one WebSocket cannot reach.
     *
     * @since 1.0.0
     */
    public val REPLY_CHANNEL: ViolationCode = code("reply.channel")

    /**
     * A warning: the reply of an operation cannot be paired with its request, because the server sends the request or
     * a message has no correlation id; its messages travel as any other.
     *
     * @since 1.0.0
     */
    public val REPLY_UNPAIRED: ViolationCode = code("reply.unpaired")

    /**
     * The correlation id of a message is no `$message.payload#` expression with a JSON pointer, such as one in the
     * headers, which the frames of a WebSocket do not carry.
     *
     * @since 1.0.0
     */
    public val CORRELATION_UNSUPPORTED: ViolationCode = code("correlation.unsupported")

    /**
     * A security scheme cannot authorize a WebSocket handshake, such as `userPassword`, or is written inline without a
     * name the runtime could select it by.
     *
     * @since 1.0.0
     */
    public val SECURITY_UNSUPPORTED: ViolationCode = code("security.unsupported")

    /**
     * The operations of one channel demand different security, but one handshake authorizes them all.
     *
     * @since 1.0.0
     */
    public val SECURITY_MIXED: ViolationCode = code("security.mixed")

    /**
     * Two declarations of the document become the same Kotlin name.
     *
     * @since 1.0.0
     */
    public val NAME_COLLISION: ViolationCode = code("name.collision")

    /**
     * An option names an artifact the AsyncAPI generator does not generate, such as mocks.
     *
     * @since 1.0.0
     */
    public val ARTIFACT_UNSUPPORTED: ViolationCode = code("options.artifact-unsupported")

    /**
     * Every code, in the order of their declaration.
     *
     * @since 1.0.0
     */
    public val all: List<ViolationCode> = listOf(
        PROTOCOL_UNSUPPORTED,
        CHANNEL_SKIPPED,
        CHANNEL_ADDRESS,
        BINDING_METHOD,
        HANDSHAKE_UNSUPPORTED,
        HANDSHAKE_RESERVED,
        SCHEMA_FORMAT,
        SCHEMA_UNSUPPORTED,
        SCHEMA_REFERENCE_SIBLING,
        REFERENCE_UNSUPPORTED,
        CONTENT_TYPE_UNSUPPORTED,
        CONTENT_TYPE_ASSUMED,
        MESSAGE_INDISTINCT,
        MESSAGE_HEADERS,
        MESSAGE_FOREIGN,
        PAYLOAD_UNSUPPORTED,
        OPERATION_CHANNEL,
        REPLY_CHANNEL,
        REPLY_UNPAIRED,
        CORRELATION_UNSUPPORTED,
        SECURITY_UNSUPPORTED,
        SECURITY_MIXED,
        NAME_COLLISION,
        ARTIFACT_UNSUPPORTED,
    )

    private fun code(name: String): ViolationCode = ViolationCode("codegen.asyncapi.$name")
}
