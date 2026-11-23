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

package com.davils.kapi.spec.openapi

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the changes [OpenApi.compare] reports between two versions of an OpenAPI document, each named
 * `openapi.compatibility.<rule>`.
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
     * A path is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val PATH_REMOVED: ViolationCode = code("path-removed")

    /**
     * A path is added; breaking for providers, which do not serve it yet.
     *
     * @since 1.0.0
     */
    public val PATH_ADDED: ViolationCode = code("path-added")

    /**
     * A path keeps its shape but names its parameters differently.
     *
     * @since 1.0.0
     */
    public val PATH_PARAMETER_RENAMED: ViolationCode = code("path-parameter-renamed")

    /**
     * An operation of a path is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val OPERATION_REMOVED: ViolationCode = code("operation-removed")

    /**
     * An operation is added to a path; breaking for providers.
     *
     * @since 1.0.0
     */
    public val OPERATION_ADDED: ViolationCode = code("operation-added")

    /**
     * The `operationId` of an operation changes, which renames the generated code on both sides.
     *
     * @since 1.0.0
     */
    public val OPERATION_ID_CHANGED: ViolationCode = code("operation-id-changed")

    /**
     * An operation is now deprecated; information only.
     *
     * @since 1.0.0
     */
    public val OPERATION_DEPRECATED: ViolationCode = code("operation-deprecated")

    /**
     * A parameter of an operation is removed; breaking for clients that still send it.
     *
     * @since 1.0.0
     */
    public val PARAMETER_REMOVED: ViolationCode = code("parameter-removed")

    /**
     * An optional parameter is added to an operation.
     *
     * @since 1.0.0
     */
    public val PARAMETER_ADDED: ViolationCode = code("parameter-added")

    /**
     * A required parameter is added to an operation; breaking for clients.
     *
     * @since 1.0.0
     */
    public val PARAMETER_REQUIRED_ADDED: ViolationCode = code("parameter-required-added")

    /**
     * An optional parameter becomes required; breaking for clients.
     *
     * @since 1.0.0
     */
    public val PARAMETER_REQUIRED: ViolationCode = code("parameter-required")

    /**
     * A required parameter becomes optional; breaking for providers.
     *
     * @since 1.0.0
     */
    public val PARAMETER_OPTIONAL: ViolationCode = code("parameter-optional")

    /**
     * The style or the explosion of a parameter changes; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val PARAMETER_SERIALIZATION: ViolationCode = code("parameter-serialization")

    /**
     * A parameter is now deprecated; information only.
     *
     * @since 1.0.0
     */
    public val PARAMETER_DEPRECATED: ViolationCode = code("parameter-deprecated")

    /**
     * A request body is added to an operation; breaking for clients if it is required.
     *
     * @since 1.0.0
     */
    public val REQUEST_BODY_ADDED: ViolationCode = code("request-body-added")

    /**
     * The request body of an operation is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val REQUEST_BODY_REMOVED: ViolationCode = code("request-body-removed")

    /**
     * An optional request body becomes required; breaking for clients.
     *
     * @since 1.0.0
     */
    public val REQUEST_BODY_REQUIRED: ViolationCode = code("request-body-required")

    /**
     * A required request body becomes optional; breaking for providers.
     *
     * @since 1.0.0
     */
    public val REQUEST_BODY_OPTIONAL: ViolationCode = code("request-body-optional")

    /**
     * A media type of a request body is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val REQUEST_MEDIA_TYPE_REMOVED: ViolationCode = code("request-media-type-removed")

    /**
     * A media type is added to a request body; breaking for providers.
     *
     * @since 1.0.0
     */
    public val REQUEST_MEDIA_TYPE_ADDED: ViolationCode = code("request-media-type-added")

    /**
     * A response status of an operation is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val RESPONSE_STATUS_REMOVED: ViolationCode = code("response-status-removed")

    /**
     * A response status is added to an operation.
     *
     * @since 1.0.0
     */
    public val RESPONSE_STATUS_ADDED: ViolationCode = code("response-status-added")

    /**
     * A media type of a response is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val RESPONSE_MEDIA_TYPE_REMOVED: ViolationCode = code("response-media-type-removed")

    /**
     * A media type is added to a response; breaking for providers.
     *
     * @since 1.0.0
     */
    public val RESPONSE_MEDIA_TYPE_ADDED: ViolationCode = code("response-media-type-added")

    /**
     * A header of a response is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val RESPONSE_HEADER_REMOVED: ViolationCode = code("response-header-removed")

    /**
     * An operation that a request could reach without security now requires it; breaking for clients.
     *
     * @since 1.0.0
     */
    public val SECURITY_ADDED: ViolationCode = code("security-added")

    /**
     * An operation no longer requires security; breaking for providers.
     *
     * @since 1.0.0
     */
    public val SECURITY_REMOVED: ViolationCode = code("security-removed")

    /**
     * An alternative of the security requirements of an operation is removed; breaking for clients.
     *
     * @since 1.0.0
     */
    public val SECURITY_REQUIREMENT_REMOVED: ViolationCode = code("security-requirement-removed")

    /**
     * An alternative is added to the security requirements of an operation; breaking for providers.
     *
     * @since 1.0.0
     */
    public val SECURITY_REQUIREMENT_ADDED: ViolationCode = code("security-requirement-added")

    /**
     * A security requirement needs a scope it did not need before; breaking for clients.
     *
     * @since 1.0.0
     */
    public val SECURITY_SCOPE_ADDED: ViolationCode = code("security-scope-added")

    /**
     * A security requirement no longer needs a scope; breaking for providers.
     *
     * @since 1.0.0
     */
    public val SECURITY_SCOPE_REMOVED: ViolationCode = code("security-scope-removed")

    /**
     * A security scheme of the components changes its type, location, name or endpoints; breaking for both sides.
     *
     * @since 1.0.0
     */
    public val SECURITY_SCHEME_CHANGED: ViolationCode = code("security-scheme-changed")

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
        PATH_REMOVED,
        PATH_ADDED,
        PATH_PARAMETER_RENAMED,
        OPERATION_REMOVED,
        OPERATION_ADDED,
        OPERATION_ID_CHANGED,
        OPERATION_DEPRECATED,
        PARAMETER_REMOVED,
        PARAMETER_ADDED,
        PARAMETER_REQUIRED_ADDED,
        PARAMETER_REQUIRED,
        PARAMETER_OPTIONAL,
        PARAMETER_SERIALIZATION,
        PARAMETER_DEPRECATED,
        REQUEST_BODY_ADDED,
        REQUEST_BODY_REMOVED,
        REQUEST_BODY_REQUIRED,
        REQUEST_BODY_OPTIONAL,
        REQUEST_MEDIA_TYPE_REMOVED,
        REQUEST_MEDIA_TYPE_ADDED,
        RESPONSE_STATUS_REMOVED,
        RESPONSE_STATUS_ADDED,
        RESPONSE_MEDIA_TYPE_REMOVED,
        RESPONSE_MEDIA_TYPE_ADDED,
        RESPONSE_HEADER_REMOVED,
        SECURITY_ADDED,
        SECURITY_REMOVED,
        SECURITY_REQUIREMENT_REMOVED,
        SECURITY_REQUIREMENT_ADDED,
        SECURITY_SCOPE_ADDED,
        SECURITY_SCOPE_REMOVED,
        SECURITY_SCHEME_CHANGED,
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

    private fun code(rule: String): ViolationCode = ViolationCode("openapi.compatibility.$rule")
}
