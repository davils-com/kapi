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
 * The codes of the changes [AsyncApi.compare] reports between two versions of an AsyncAPI document, each named
 * `asyncapi.compatibility.<rule>`.
 *
 * Codes are stable API: a tool may react to them or acknowledge them. Every change also says what it means for
 * clients and for providers; the rules of the schemas judge a narrower schema by the direction the data flows in,
 * so a narrower request breaks clients and a narrower response breaks providers.
 *
 * @since 1.0.0
 */
public object CompatibilityCodes {
    /**
     * A server of the document is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val SERVER_REMOVED: ViolationCode = code("server-removed")

    /**
     * A server is added to the document.
     *
     * @since 1.0.0
     */
    public val SERVER_ADDED: ViolationCode = code("server-added")

    /**
     * The host, the path name or the protocol of a server changes; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val SERVER_ADDRESS_CHANGED: ViolationCode = code("server-address-changed")

    /**
     * The bindings of a server change; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val SERVER_BINDINGS_CHANGED: ViolationCode = code("server-bindings-changed")

    /**
     * A channel of the document is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val CHANNEL_REMOVED: ViolationCode = code("channel-removed")

    /**
     * A channel is added to the document.
     *
     * @since 1.0.0
     */
    public val CHANNEL_ADDED: ViolationCode = code("channel-added")

    /**
     * The address of a channel changes; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val CHANNEL_ADDRESS_CHANGED: ViolationCode = code("channel-address-changed")

    /**
     * The bindings of a channel change; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val CHANNEL_BINDINGS_CHANGED: ViolationCode = code("channel-bindings-changed")

    /**
     * An operation of the document is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val OPERATION_REMOVED: ViolationCode = code("operation-removed")

    /**
     * An operation is added to the document; breaking for providers.
     *
     * @since 1.0.0
     */
    public val OPERATION_ADDED: ViolationCode = code("operation-added")

    /**
     * An operation changes between sending and receiving; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val OPERATION_ACTION_CHANGED: ViolationCode = code("operation-action-changed")

    /**
     * An operation moves to a channel of another address; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val OPERATION_CHANNEL_CHANGED: ViolationCode = code("operation-channel-changed")

    /**
     * The bindings of an operation change; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val OPERATION_BINDINGS_CHANGED: ViolationCode = code("operation-bindings-changed")

    /**
     * A message is removed from an operation; breaking for the side that sends it.
     *
     * @since 1.0.0
     */
    public val MESSAGE_REMOVED: ViolationCode = code("message-removed")

    /**
     * A message is added to an operation; breaking for the side that receives it.
     *
     * @since 1.0.0
     */
    public val MESSAGE_ADDED: ViolationCode = code("message-added")

    /**
     * The content type of a message changes; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val MESSAGE_CONTENT_TYPE_CHANGED: ViolationCode = code("message-content-type-changed")

    /**
     * The bindings of a message change; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val MESSAGE_BINDINGS_CHANGED: ViolationCode = code("message-bindings-changed")

    /**
     * An operation or a server that could be used without security now requires it; breaking for clients.
     *
     * @since 1.0.0
     */
    public val SECURITY_ADDED: ViolationCode = code("security-added")

    /**
     * An operation or a server no longer requires security; breaking for providers.
     *
     * @since 1.0.0
     */
    public val SECURITY_REMOVED: ViolationCode = code("security-removed")

    /**
     * A schema allows fewer types than before, such as `string` where `string` or `integer` was allowed.
     *
     * @since 1.0.0
     */
    public val TYPE_NARROWED: ViolationCode = code("type-narrowed")

    /**
     * A schema allows more types than before.
     *
     * @since 1.0.0
     */
    public val TYPE_WIDENED: ViolationCode = code("type-widened")

    /**
     * A schema allows other types than before, some fewer and some more.
     *
     * @since 1.0.0
     */
    public val TYPE_CHANGED: ViolationCode = code("type-changed")

    /**
     * A schema no longer allows `null`.
     *
     * @since 1.0.0
     */
    public val NULLABLE_REMOVED: ViolationCode = code("nullable-removed")

    /**
     * A schema now allows `null`.
     *
     * @since 1.0.0
     */
    public val NULLABLE_ADDED: ViolationCode = code("nullable-added")

    /**
     * A schema changes between accepting everything, accepting nothing and a schema object.
     *
     * @since 1.0.0
     */
    public val ACCEPTS: ViolationCode = code("accepts")

    /**
     * A schema of a format the comparison does not read, such as Avro, changes.
     *
     * @since 1.0.0
     */
    public val OPAQUE: ViolationCode = code("opaque")

    /**
     * The `maxLength` of a string drops, rises, is added or is removed.
     *
     * @since 1.0.0
     */
    public val MAX_LENGTH: ViolationCode = code("max-length")

    /**
     * The `minLength` of a string rises, drops, is added or is removed.
     *
     * @since 1.0.0
     */
    public val MIN_LENGTH: ViolationCode = code("min-length")

    /**
     * The upper bound of a number drops, rises, becomes exclusive, is added or is removed.
     *
     * @since 1.0.0
     */
    public val MAXIMUM: ViolationCode = code("maximum")

    /**
     * The lower bound of a number rises, drops, becomes exclusive, is added or is removed.
     *
     * @since 1.0.0
     */
    public val MINIMUM: ViolationCode = code("minimum")

    /**
     * The `maxItems` of an array changes.
     *
     * @since 1.0.0
     */
    public val MAX_ITEMS: ViolationCode = code("max-items")

    /**
     * The `minItems` of an array changes.
     *
     * @since 1.0.0
     */
    public val MIN_ITEMS: ViolationCode = code("min-items")

    /**
     * The `maxProperties` of an object changes.
     *
     * @since 1.0.0
     */
    public val MAX_PROPERTIES: ViolationCode = code("max-properties")

    /**
     * The `minProperties` of an object changes.
     *
     * @since 1.0.0
     */
    public val MIN_PROPERTIES: ViolationCode = code("min-properties")

    /**
     * The `multipleOf` of a number is added, removed or changed.
     *
     * @since 1.0.0
     */
    public val MULTIPLE_OF: ViolationCode = code("multiple-of")

    /**
     * The `pattern` of a string is added, removed or changed.
     *
     * @since 1.0.0
     */
    public val PATTERN: ViolationCode = code("pattern")

    /**
     * The `format` of a value is added, removed or changed.
     *
     * @since 1.0.0
     */
    public val FORMAT: ViolationCode = code("format")

    /**
     * The items of an array must now or need no longer be unique.
     *
     * @since 1.0.0
     */
    public val UNIQUE_ITEMS: ViolationCode = code("unique-items")

    /**
     * An `enum` is added to or removed from a schema.
     *
     * @since 1.0.0
     */
    public val ENUM: ViolationCode = code("enum")

    /**
     * An `enum` no longer allows some of its values.
     *
     * @since 1.0.0
     */
    public val ENUM_VALUE_REMOVED: ViolationCode = code("enum-value-removed")

    /**
     * An `enum` allows values it did not allow before.
     *
     * @since 1.0.0
     */
    public val ENUM_VALUE_ADDED: ViolationCode = code("enum-value-added")

    /**
     * A `const` is added, removed or changed.
     *
     * @since 1.0.0
     */
    public val CONST: ViolationCode = code("const")

    /**
     * The property of the `discriminator` of a schema changes.
     *
     * @since 1.0.0
     */
    public val DISCRIMINATOR: ViolationCode = code("discriminator")

    /**
     * A schema is now deprecated; information only.
     *
     * @since 1.0.0
     */
    public val DEPRECATED: ViolationCode = code("deprecated")

    /**
     * A property of an object is removed.
     *
     * @since 1.0.0
     */
    public val PROPERTY_REMOVED: ViolationCode = code("property-removed")

    /**
     * An optional property is added to an object.
     *
     * @since 1.0.0
     */
    public val PROPERTY_ADDED: ViolationCode = code("property-added")

    /**
     * A required property is added to an object.
     *
     * @since 1.0.0
     */
    public val REQUIRED_PROPERTY_ADDED: ViolationCode = code("required-property-added")

    /**
     * A property of an object becomes required.
     *
     * @since 1.0.0
     */
    public val PROPERTY_REQUIRED: ViolationCode = code("property-required")

    /**
     * A required property of an object becomes optional.
     *
     * @since 1.0.0
     */
    public val PROPERTY_OPTIONAL: ViolationCode = code("property-optional")

    /**
     * An object no longer allows or now allows properties it does not name.
     *
     * @since 1.0.0
     */
    public val ADDITIONAL_PROPERTIES: ViolationCode = code("additional-properties")

    /**
     * The items of an array are now or no longer described by a schema.
     *
     * @since 1.0.0
     */
    public val ITEMS: ViolationCode = code("items")

    /**
     * The number of the schemas of an `allOf` changes.
     *
     * @since 1.0.0
     */
    public val ALL_OF: ViolationCode = code("all-of")

    /**
     * The number of the schemas of an `anyOf` changes.
     *
     * @since 1.0.0
     */
    public val ANY_OF: ViolationCode = code("any-of")

    /**
     * The number of the schemas of a `oneOf` changes.
     *
     * @since 1.0.0
     */
    public val ONE_OF: ViolationCode = code("one-of")

    /**
     * The comparison could not look at a part of the documents within its limits, or dropped changes beyond them;
     * breaking for both sides, because nothing is known about that part.
     *
     * @since 1.0.0
     */
    public val INCOMPLETE: ViolationCode = code("incomplete")

    /**
     * Every code of the catalogue, in the order of their declaration.
     *
     * @since 1.0.0
     */
    public val all: List<ViolationCode> = listOf(
        SERVER_REMOVED,
        SERVER_ADDED,
        SERVER_ADDRESS_CHANGED,
        SERVER_BINDINGS_CHANGED,
        CHANNEL_REMOVED,
        CHANNEL_ADDED,
        CHANNEL_ADDRESS_CHANGED,
        CHANNEL_BINDINGS_CHANGED,
        OPERATION_REMOVED,
        OPERATION_ADDED,
        OPERATION_ACTION_CHANGED,
        OPERATION_CHANNEL_CHANGED,
        OPERATION_BINDINGS_CHANGED,
        MESSAGE_REMOVED,
        MESSAGE_ADDED,
        MESSAGE_CONTENT_TYPE_CHANGED,
        MESSAGE_BINDINGS_CHANGED,
        SECURITY_ADDED,
        SECURITY_REMOVED,
        TYPE_NARROWED,
        TYPE_WIDENED,
        TYPE_CHANGED,
        NULLABLE_REMOVED,
        NULLABLE_ADDED,
        ACCEPTS,
        OPAQUE,
        MAX_LENGTH,
        MIN_LENGTH,
        MAXIMUM,
        MINIMUM,
        MAX_ITEMS,
        MIN_ITEMS,
        MAX_PROPERTIES,
        MIN_PROPERTIES,
        MULTIPLE_OF,
        PATTERN,
        FORMAT,
        UNIQUE_ITEMS,
        ENUM,
        ENUM_VALUE_REMOVED,
        ENUM_VALUE_ADDED,
        CONST,
        DISCRIMINATOR,
        DEPRECATED,
        PROPERTY_REMOVED,
        PROPERTY_ADDED,
        REQUIRED_PROPERTY_ADDED,
        PROPERTY_REQUIRED,
        PROPERTY_OPTIONAL,
        ADDITIONAL_PROPERTIES,
        ITEMS,
        ALL_OF,
        ANY_OF,
        ONE_OF,
        INCOMPLETE,
    )

    private fun code(rule: String): ViolationCode = ViolationCode("asyncapi.compatibility.$rule")
}
