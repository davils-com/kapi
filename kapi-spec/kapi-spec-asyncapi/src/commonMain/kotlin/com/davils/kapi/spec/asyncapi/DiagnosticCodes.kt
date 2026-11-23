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

package com.davils.kapi.spec.asyncapi

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the findings an AsyncAPI reading reports, each in the namespace `asyncapi.<area>.<rule>`.
 *
 * Codes are stable API: a code keeps its meaning across releases. A finding is an error unless its code is a warning
 * by nature, such as [REFERENCE_SIBLING]; the codes a severity policy may downgrade are named in their documentation,
 * every other code guards the reader or the meaning of the document and keeps its severity.
 *
 * @since 1.0.0
 */
public object DiagnosticCodes {
    /**
     * A document is no valid UTF-8.
     *
     * @since 1.0.0
     */
    public val SOURCE_ENCODING: ViolationCode = code("source.encoding")

    /**
     * A source holds more than one YAML document.
     *
     * @since 1.0.0
     */
    public val SOURCE_MULTIPLE_DOCUMENTS: ViolationCode = code("source.multiple-documents")

    /**
     * A key of a mapping is no scalar.
     *
     * @since 1.0.0
     */
    public val SOURCE_KEY_NOT_SCALAR: ViolationCode = code("source.key-not-scalar")

    /**
     * A value is one that JSON cannot express, such as a timestamp of YAML 1.1.
     *
     * @since 1.0.0
     */
    public val SOURCE_NON_JSON_VALUE: ViolationCode = code("source.non-json-value")

    /**
     * A merge key `<<` stands in a document that is read without merge keys.
     *
     * @since 1.0.0
     */
    public val SOURCE_MERGE_KEY: ViolationCode = code("source.merge-key")

    /**
     * The document is empty or no object.
     *
     * @since 1.0.0
     */
    public val DOCUMENT_NOT_MAPPING: ViolationCode = code("document.not-mapping")

    /**
     * The `id` of the document is no absolute URI.
     *
     * @since 1.0.0
     */
    public val DOCUMENT_ID: ViolationCode = code("document.id")

    /**
     * The document names no version in its field `asyncapi`.
     *
     * @since 1.0.0
     */
    public val VERSION_MISSING: ViolationCode = code("version.missing")

    /**
     * The document names a version kapi does not read, such as AsyncAPI 2.6.0.
     *
     * @since 1.0.0
     */
    public val VERSION_UNSUPPORTED: ViolationCode = code("version.unsupported")

    /**
     * The version names no patch, such as `3.0`; a severity policy may downgrade it.
     *
     * @since 1.0.0
     */
    public val VERSION_PATCH_MISSING: ViolationCode = code("version.patch-missing")

    /**
     * A required field is missing.
     *
     * @since 1.0.0
     */
    public val FIELD_MISSING: ViolationCode = code("field.missing")

    /**
     * A field has a value of the wrong type.
     *
     * @since 1.0.0
     */
    public val FIELD_TYPE: ViolationCode = code("field.type")

    /**
     * A field is no field of its object; a severity policy may downgrade it.
     *
     * @since 1.0.0
     */
    public val FIELD_UNKNOWN: ViolationCode = code("field.unknown")

    /**
     * A field belongs to another version of AsyncAPI, such as `publish` of AsyncAPI 2; a severity policy may
     * downgrade it.
     *
     * @since 1.0.0
     */
    public val FIELD_VERSION: ViolationCode = code("field.version")

    /**
     * A reference points at nothing.
     *
     * @since 1.0.0
     */
    public val REFERENCE_UNRESOLVED: ViolationCode = code("reference.unresolved")

    /**
     * A reference leads back to itself through other references.
     *
     * @since 1.0.0
     */
    public val REFERENCE_CYCLE: ViolationCode = code("reference.cycle")

    /**
     * A reference passes through more references than a reading follows.
     *
     * @since 1.0.0
     */
    public val REFERENCE_CHAIN_TOO_LONG: ViolationCode = code("reference.chain-too-long")

    /**
     * A field stands beside a reference and is ignored; a warning a severity policy may downgrade.
     *
     * @since 1.0.0
     */
    public val REFERENCE_SIBLING: ViolationCode = code("reference.sibling")

    /**
     * The fragment of a reference is no JSON Pointer.
     *
     * @since 1.0.0
     */
    public val REFERENCE_POINTER: ViolationCode = code("reference.pointer")

    /**
     * A reference is no string or no URI reference.
     *
     * @since 1.0.0
     */
    public val REFERENCE_SYNTAX: ViolationCode = code("reference.syntax")

    /**
     * A relative reference stands in a document without a hierarchical address to resolve it against.
     *
     * @since 1.0.0
     */
    public val REFERENCE_NO_BASE: ViolationCode = code("reference.no-base")

    /**
     * A reference leads outside the folder references may reach.
     *
     * @since 1.0.0
     */
    public val REFERENCE_OUTSIDE: ViolationCode = code("reference.outside")

    /**
     * One place is referenced as two different objects, such as a channel and a message.
     *
     * @since 1.0.0
     */
    public val REFERENCE_KIND: ViolationCode = code("reference.kind")

    /**
     * A field that must be a reference, such as the `channel` of an operation, is written inline.
     *
     * @since 1.0.0
     */
    public val REFERENCE_REQUIRED: ViolationCode = code("reference.required")

    /**
     * A referenced document does not exist.
     *
     * @since 1.0.0
     */
    public val LOAD_MISSING: ViolationCode = code("load.missing")

    /**
     * A document is larger than a reading allows.
     *
     * @since 1.0.0
     */
    public val LOAD_SIZE: ViolationCode = code("load.size")

    /**
     * A reading would load more documents than it allows.
     *
     * @since 1.0.0
     */
    public val LOAD_DOCUMENTS: ViolationCode = code("load.documents")

    /**
     * A reference names a remote document, which a reading never loads.
     *
     * @since 1.0.0
     */
    public val LOAD_REMOTE: ViolationCode = code("load.remote")

    /**
     * A reference names a document of another scheme than the root document.
     *
     * @since 1.0.0
     */
    public val LOAD_SCHEME: ViolationCode = code("load.scheme")

    /**
     * The resolver failed to load a document.
     *
     * @since 1.0.0
     */
    public val LOAD_FAILED: ViolationCode = code("load.failed")

    /**
     * A reading found more than it reports; the finding says how many were dropped.
     *
     * @since 1.0.0
     */
    public val LIMIT_DIAGNOSTICS: ViolationCode = code("limit.diagnostics")

    /**
     * A key of a map, such as the name of a component, a channel or an operation, breaks the pattern AsyncAPI gives
     * it; a severity policy may downgrade it.
     *
     * @since 1.0.0
     */
    public val KEY_INVALID: ViolationCode = code("key.invalid")

    /**
     * The host or the path of a server is no valid template of variables.
     *
     * @since 1.0.0
     */
    public val SERVER_TEMPLATE: ViolationCode = code("server.template")

    /**
     * The host or the path of a server names a variable the server does not declare.
     *
     * @since 1.0.0
     */
    public val SERVER_VARIABLE_UNDECLARED: ViolationCode = code("server.variable-undeclared")

    /**
     * The default of a server variable is none of its allowed values; a severity policy may downgrade it.
     *
     * @since 1.0.0
     */
    public val SERVER_VARIABLE_DEFAULT: ViolationCode = code("server.variable-default")

    /**
     * The address of a channel is no valid template of parameters.
     *
     * @since 1.0.0
     */
    public val CHANNEL_ADDRESS_TEMPLATE: ViolationCode = code("channel.address-template")

    /**
     * The address of a channel names a parameter the channel does not declare.
     *
     * @since 1.0.0
     */
    public val CHANNEL_PARAMETER_UNDECLARED: ViolationCode = code("channel.parameter-undeclared")

    /**
     * A channel declares a parameter its address does not name.
     *
     * @since 1.0.0
     */
    public val CHANNEL_PARAMETER_UNUSED: ViolationCode = code("channel.parameter-unused")

    /**
     * A channel of the root `channels` names a server outside the root `servers`.
     *
     * @since 1.0.0
     */
    public val CHANNEL_SERVER_PLACEMENT: ViolationCode = code("channel.server-placement")

    /**
     * The `action` of an operation is neither `send` nor `receive`.
     *
     * @since 1.0.0
     */
    public val OPERATION_ACTION: ViolationCode = code("operation.action")

    /**
     * An operation of the root `operations` names a channel outside the root `channels`.
     *
     * @since 1.0.0
     */
    public val OPERATION_CHANNEL_PLACEMENT: ViolationCode = code("operation.channel-placement")

    /**
     * An operation names a message that is no message of its channel.
     *
     * @since 1.0.0
     */
    public val OPERATION_MESSAGE_NOT_IN_CHANNEL: ViolationCode = code("operation.message-not-in-channel")

    /**
     * A reply names a message that is no message of its channel.
     *
     * @since 1.0.0
     */
    public val REPLY_MESSAGE_NOT_IN_CHANNEL: ViolationCode = code("reply.message-not-in-channel")

    /**
     * A reply has an address and a channel whose address is set as well.
     *
     * @since 1.0.0
     */
    public val REPLY_ADDRESS_CONFLICT: ViolationCode = code("reply.address-conflict")

    /**
     * The headers of a message are described by a schema of another type than `object`.
     *
     * @since 1.0.0
     */
    public val MESSAGE_HEADERS_TYPE: ViolationCode = code("message.headers-type")

    /**
     * An example of a message has neither headers nor a payload; a severity policy may downgrade it.
     *
     * @since 1.0.0
     */
    public val MESSAGE_EXAMPLE_EMPTY: ViolationCode = code("message.example-empty")

    /**
     * A content type, of a message or the default of the document, is no media type; a severity policy may
     * downgrade it.
     *
     * @since 1.0.0
     */
    public val MESSAGE_CONTENT_TYPE: ViolationCode = code("message.content-type")

    /**
     * A runtime expression, such as the `location` of a correlation id, is no valid expression such as
     * `$message.header#/correlationId`.
     *
     * @since 1.0.0
     */
    public val RUNTIME_EXPRESSION: ViolationCode = code("runtime-expression.syntax")

    /**
     * A schema names a format kapi does not know; a warning a severity policy may downgrade.
     *
     * @since 1.0.0
     */
    public val SCHEMA_FORMAT_UNKNOWN: ViolationCode = code("schema.format-unknown")

    /**
     * A security scheme has no type or one AsyncAPI does not define.
     *
     * @since 1.0.0
     */
    public val SECURITY_TYPE: ViolationCode = code("security.type")

    /**
     * An api key names no place AsyncAPI allows for it.
     *
     * @since 1.0.0
     */
    public val SECURITY_LOCATION: ViolationCode = code("security.location")

    /**
     * A security scheme requires a scope none of its flows offers; a severity policy may downgrade it.
     *
     * @since 1.0.0
     */
    public val SECURITY_SCOPES: ViolationCode = code("security.scopes")

    /**
     * The `method` of a WebSocket channel binding is neither `GET` nor `POST`.
     *
     * @since 1.0.0
     */
    public val BINDING_WEBSOCKET_METHOD: ViolationCode = code("binding.websocket-method")

    /**
     * The `query` or `headers` of a WebSocket channel binding is described by a schema of another type than
     * `object`.
     *
     * @since 1.0.0
     */
    public val BINDING_WEBSOCKET_SCHEMA: ViolationCode = code("binding.websocket-schema")

    /**
     * Two tags of one list share a name; a severity policy may downgrade it.
     *
     * @since 1.0.0
     */
    public val TAG_DUPLICATE: ViolationCode = code("tag.duplicate")

    /**
     * The document breaks the official JSON Schema of AsyncAPI 3.0 at a place the reader has not reported; a severity
     * policy may downgrade it.
     *
     * @since 1.0.0
     */
    public val STRUCTURE_SCHEMA: ViolationCode = code("structure.schema")

    /**
     * One operation is listed under more than one key of the root `operations`, so it has more than one id.
     *
     * @since 1.0.0
     */
    public val OPERATION_KEY_DUPLICATE: ViolationCode = code("operation.key-duplicate")

    /**
     * A security requirement of a server or an operation refers to something else than a security scheme of
     * `components/securitySchemes`.
     *
     * @since 1.0.0
     */
    public val SECURITY_REFERENCE_PLACEMENT: ViolationCode = code("security.reference-placement")

    /**
     * Every code of the catalogue, in the order of their declaration.
     *
     * @since 1.0.0
     */
    public val all: List<ViolationCode> = listOf(
        SOURCE_ENCODING,
        SOURCE_MULTIPLE_DOCUMENTS,
        SOURCE_KEY_NOT_SCALAR,
        SOURCE_NON_JSON_VALUE,
        SOURCE_MERGE_KEY,
        DOCUMENT_NOT_MAPPING,
        DOCUMENT_ID,
        VERSION_MISSING,
        VERSION_UNSUPPORTED,
        VERSION_PATCH_MISSING,
        FIELD_MISSING,
        FIELD_TYPE,
        FIELD_UNKNOWN,
        FIELD_VERSION,
        REFERENCE_UNRESOLVED,
        REFERENCE_CYCLE,
        REFERENCE_CHAIN_TOO_LONG,
        REFERENCE_SIBLING,
        REFERENCE_POINTER,
        REFERENCE_SYNTAX,
        REFERENCE_NO_BASE,
        REFERENCE_OUTSIDE,
        REFERENCE_KIND,
        REFERENCE_REQUIRED,
        LOAD_MISSING,
        LOAD_SIZE,
        LOAD_DOCUMENTS,
        LOAD_REMOTE,
        LOAD_SCHEME,
        LOAD_FAILED,
        LIMIT_DIAGNOSTICS,
        KEY_INVALID,
        SERVER_TEMPLATE,
        SERVER_VARIABLE_UNDECLARED,
        SERVER_VARIABLE_DEFAULT,
        CHANNEL_ADDRESS_TEMPLATE,
        CHANNEL_PARAMETER_UNDECLARED,
        CHANNEL_PARAMETER_UNUSED,
        CHANNEL_SERVER_PLACEMENT,
        OPERATION_ACTION,
        OPERATION_CHANNEL_PLACEMENT,
        OPERATION_MESSAGE_NOT_IN_CHANNEL,
        REPLY_MESSAGE_NOT_IN_CHANNEL,
        REPLY_ADDRESS_CONFLICT,
        MESSAGE_HEADERS_TYPE,
        MESSAGE_EXAMPLE_EMPTY,
        MESSAGE_CONTENT_TYPE,
        RUNTIME_EXPRESSION,
        SCHEMA_FORMAT_UNKNOWN,
        SECURITY_TYPE,
        SECURITY_LOCATION,
        SECURITY_SCOPES,
        BINDING_WEBSOCKET_METHOD,
        BINDING_WEBSOCKET_SCHEMA,
        TAG_DUPLICATE,
        STRUCTURE_SCHEMA,
        OPERATION_KEY_DUPLICATE,
        SECURITY_REFERENCE_PLACEMENT,
    )

    private fun code(name: String): ViolationCode = ViolationCode("asyncapi.$name")
}
