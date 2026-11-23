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
 * The codes of the findings of an OpenAPI reading, each named `openapi.<area>.<rule>`.
 *
 * Codes are stable API: a tool may react to them, acknowledge or downgrade them. Most findings are errors; the
 * severity of each code and which codes may be downgraded are fixed by the reader.
 *
 * @since 1.0.0
 */
public object DiagnosticCodes {
    /**
     * A document is not valid UTF-8.
     *
     * @since 1.0.0
     */
    public val SOURCE_ENCODING: ViolationCode = code("source.encoding")

    /**
     * A source holds more than one YAML document.
     *
     * @since 1.0.0
     */
    public val SOURCE_MULTIPLE_DOCUMENTS: ViolationCode = ViolationCode(
        "openapi.source.multiple-documents",
    )

    /**
     * A mapping key is no scalar, which JSON cannot express.
     *
     * @since 1.0.0
     */
    public val SOURCE_KEY_NOT_SCALAR: ViolationCode = code("source.key-not-scalar")

    /**
     * A value has a YAML type that JSON cannot express, such as a timestamp or binary data.
     *
     * @since 1.0.0
     */
    public val SOURCE_NON_JSON_VALUE: ViolationCode = code("source.non-json-value")

    /**
     * A mapping uses the YAML 1.1 merge key `<<`, which a reader honours only when configured to.
     *
     * @since 1.0.0
     */
    public val SOURCE_MERGE_KEY: ViolationCode = code("source.merge-key")

    /**
     * A document is no object, or a source holds no document at all.
     *
     * @since 1.0.0
     */
    public val DOCUMENT_NOT_MAPPING: ViolationCode = code("document.not-mapping")

    /**
     * The document names no version in its field `openapi`.
     *
     * @since 1.0.0
     */
    public val VERSION_MISSING: ViolationCode = code("version.missing")

    /**
     * The document is a Swagger document of a version other than 2.0.
     *
     * @since 1.0.0
     */
    public val VERSION_SWAGGER: ViolationCode = code("version.swagger")

    /**
     * The document names a version of OpenAPI other than 3.0, 3.1 and 3.2.
     *
     * @since 1.0.0
     */
    public val VERSION_UNSUPPORTED: ViolationCode = code("version.unsupported")

    /**
     * The version of the document names no patch, such as `3.1` instead of `3.1.1`.
     *
     * @since 1.0.0
     */
    public val VERSION_PATCH_MISSING: ViolationCode = code("version.patch-missing")

    /**
     * A Swagger 2.0 document was upgraded to OpenAPI 3.1 before it was read.
     *
     * @since 1.0.0
     */
    public val VERSION_UPGRADED: ViolationCode = code("version.upgraded")

    /**
     * The upgrade of a Swagger 2.0 document could not keep a field.
     *
     * @since 1.0.0
     */
    public val UPGRADE_LOSSY: ViolationCode = code("upgrade.lossy")

    /**
     * An overlay names no version of the Overlay specification the reader knows.
     *
     * @since 1.0.0
     */
    public val OVERLAY_VERSION: ViolationCode = code("overlay.version")

    /**
     * An overlay is not shaped as the Overlay specification requires.
     *
     * @since 1.0.0
     */
    public val OVERLAY_SHAPE: ViolationCode = code("overlay.shape")

    /**
     * The target of an overlay action is no valid JSONPath.
     *
     * @since 1.0.0
     */
    public val OVERLAY_TARGET: ViolationCode = code("overlay.target")

    /**
     * An overlay action neither updates nor removes, or does both.
     *
     * @since 1.0.0
     */
    public val OVERLAY_ACTION: ViolationCode = code("overlay.action")

    /**
     * The target of an overlay action matches nothing in the document.
     *
     * @since 1.0.0
     */
    public val OVERLAY_UNMATCHED: ViolationCode = code("overlay.unmatched")

    /**
     * An overlay action updates a node with a value of another kind.
     *
     * @since 1.0.0
     */
    public val OVERLAY_UPDATE: ViolationCode = code("overlay.update")

    /**
     * A required field is missing.
     *
     * @since 1.0.0
     */
    public val FIELD_MISSING: ViolationCode = code("field.missing")

    /**
     * A field holds a value of the wrong kind.
     *
     * @since 1.0.0
     */
    public val FIELD_TYPE: ViolationCode = code("field.type")

    /**
     * An object has a field the specification does not define.
     *
     * @since 1.0.0
     */
    public val FIELD_UNKNOWN: ViolationCode = code("field.unknown")

    /**
     * A field exists in another version of OpenAPI than the one of the document.
     *
     * @since 1.0.0
     */
    public val FIELD_VERSION: ViolationCode = code("field.version")

    /**
     * A reference leads to nothing.
     *
     * @since 1.0.0
     */
    public val REFERENCE_UNRESOLVED: ViolationCode = code("reference.unresolved")

    /**
     * References lead in a circle without reaching an object.
     *
     * @since 1.0.0
     */
    public val REFERENCE_CYCLE: ViolationCode = code("reference.cycle")

    /**
     * A chain of references is longer than the limit of the reading.
     *
     * @since 1.0.0
     */
    public val REFERENCE_CHAIN_TOO_LONG: ViolationCode = code("reference.chain-too-long")

    /**
     * A reference has fields beside it that the specification ignores.
     *
     * @since 1.0.0
     */
    public val REFERENCE_SIBLING: ViolationCode = code("reference.sibling")

    /**
     * The fragment of a reference is no valid JSON Pointer.
     *
     * @since 1.0.0
     */
    public val REFERENCE_POINTER: ViolationCode = code("reference.pointer")

    /**
     * A reference is no valid URI reference.
     *
     * @since 1.0.0
     */
    public val REFERENCE_SYNTAX: ViolationCode = code("reference.syntax")

    /**
     * A relative reference cannot be resolved, because its document has no place of its own.
     *
     * @since 1.0.0
     */
    public val REFERENCE_NO_BASE: ViolationCode = code("reference.no-base")

    /**
     * A reference leads to a document outside the folder references may reach.
     *
     * @since 1.0.0
     */
    public val REFERENCE_OUTSIDE: ViolationCode = code("reference.outside")

    /**
     * A reference leads to an object of another kind than its place expects.
     *
     * @since 1.0.0
     */
    public val REFERENCE_KIND: ViolationCode = code("reference.kind")

    /**
     * A reference was resolved against the `$self` of its document.
     *
     * @since 1.0.0
     */
    public val REFERENCE_REBASED: ViolationCode = code("reference.rebased")

    /**
     * A referenced document does not exist.
     *
     * @since 1.0.0
     */
    public val LOAD_MISSING: ViolationCode = code("load.missing")

    /**
     * A document is larger than the limit of the reading.
     *
     * @since 1.0.0
     */
    public val LOAD_SIZE: ViolationCode = code("load.size")

    /**
     * A reading would load more documents than its limit.
     *
     * @since 1.0.0
     */
    public val LOAD_DOCUMENTS: ViolationCode = code("load.documents")

    /**
     * A reference leads to a remote document, which no reading loads.
     *
     * @since 1.0.0
     */
    public val LOAD_REMOTE: ViolationCode = code("load.remote")

    /**
     * A reference leads to a document of another scheme than the root document.
     *
     * @since 1.0.0
     */
    public val LOAD_SCHEME: ViolationCode = code("load.scheme")

    /**
     * The resolver of the reader could not load a document.
     *
     * @since 1.0.0
     */
    public val LOAD_FAILED: ViolationCode = code("load.failed")

    /**
     * A document has more nodes than the limit of the reading.
     *
     * @since 1.0.0
     */
    public val LIMIT_NODES: ViolationCode = code("limit.nodes")

    /**
     * A document nests deeper than the limit of the reading.
     *
     * @since 1.0.0
     */
    public val LIMIT_DEPTH: ViolationCode = code("limit.depth")

    /**
     * A reading found more findings than its limit and dropped the rest.
     *
     * @since 1.0.0
     */
    public val LIMIT_DIAGNOSTICS: ViolationCode = code("limit.diagnostics")

    /**
     * A regular expression of a schema is longer than the limit of the reading.
     *
     * @since 1.0.0
     */
    public val LIMIT_PATTERN_LENGTH: ViolationCode = code("limit.pattern-length")

    /**
     * Two operations share an `operationId`.
     *
     * @since 1.0.0
     */
    public val OPERATION_ID_DUPLICATE: ViolationCode = code("operation.id-duplicate")

    /**
     * An operation declares no response.
     *
     * @since 1.0.0
     */
    public val OPERATION_RESPONSES_EMPTY: ViolationCode = ViolationCode(
        "openapi.operation.responses-empty",
    )

    /**
     * A response is keyed by no valid status code, range or `default`.
     *
     * @since 1.0.0
     */
    public val RESPONSE_STATUS: ViolationCode = code("response.status")

    /**
     * A path does not start with a slash.
     *
     * @since 1.0.0
     */
    public val PATH_NOT_ROOTED: ViolationCode = code("path.not-rooted")

    /**
     * A path template is malformed, such as an unclosed or empty `{}`.
     *
     * @since 1.0.0
     */
    public val PATH_TEMPLATE: ViolationCode = code("path.template")

    /**
     * Two path templates match the same requests.
     *
     * @since 1.0.0
     */
    public val PATH_AMBIGUOUS: ViolationCode = code("path.ambiguous")

    /**
     * A path template names a parameter no operation declares.
     *
     * @since 1.0.0
     */
    public val PATH_PARAMETER_UNDECLARED: ViolationCode = ViolationCode(
        "openapi.path.parameter-undeclared",
    )

    /**
     * A path parameter is declared but not named in the path template.
     *
     * @since 1.0.0
     */
    public val PATH_PARAMETER_UNUSED: ViolationCode = code("path.parameter-unused")

    /**
     * A parameter names a location the specification does not define.
     *
     * @since 1.0.0
     */
    public val PARAMETER_LOCATION: ViolationCode = code("parameter.location")

    /**
     * A parameter uses a style its location does not allow.
     *
     * @since 1.0.0
     */
    public val PARAMETER_STYLE: ViolationCode = code("parameter.style")

    /**
     * A parameter has both or neither of `schema` and `content`, or `content` with other than one media type.
     *
     * @since 1.0.0
     */
    public val PARAMETER_SHAPE: ViolationCode = code("parameter.shape")

    /**
     * Two parameters of one operation share a name and a location.
     *
     * @since 1.0.0
     */
    public val PARAMETER_DUPLICATE: ViolationCode = code("parameter.duplicate")

    /**
     * A path parameter is not required.
     *
     * @since 1.0.0
     */
    public val PARAMETER_PATH_OPTIONAL: ViolationCode = code("parameter.path-optional")

    /**
     * A server URL names a variable the server does not declare.
     *
     * @since 1.0.0
     */
    public val SERVER_VARIABLE_UNDECLARED: ViolationCode = ViolationCode(
        "openapi.server.variable-undeclared",
    )

    /**
     * The default of a server variable is not one of its values.
     *
     * @since 1.0.0
     */
    public val SERVER_VARIABLE_DEFAULT: ViolationCode = code("server.variable-default")

    /**
     * A security scheme has a type the specification does not define.
     *
     * @since 1.0.0
     */
    public val SECURITY_TYPE: ViolationCode = code("security.type")

    /**
     * An API key scheme names a location other than query, header and cookie.
     *
     * @since 1.0.0
     */
    public val SECURITY_LOCATION: ViolationCode = code("security.location")

    /**
     * A security requirement names a scheme the components do not declare.
     *
     * @since 1.0.0
     */
    public val SECURITY_UNKNOWN_SCHEME: ViolationCode = code("security.unknown-scheme")

    /**
     * A security requirement names scopes a scheme that is not OAuth 2 or OpenID Connect cannot have.
     *
     * @since 1.0.0
     */
    public val SECURITY_SCOPES: ViolationCode = code("security.scopes")

    /**
     * The mapping of a discriminator leads to no schema.
     *
     * @since 1.0.0
     */
    public val SCHEMA_DISCRIMINATOR_MAPPING: ViolationCode = ViolationCode(
        "openapi.schema.discriminator-mapping",
    )

    /**
     * A constraint of a schema contradicts another, such as a minimum above the maximum.
     *
     * @since 1.0.0
     */
    public val SCHEMA_CONSTRAINT: ViolationCode = code("schema.constraint")

    /**
     * A schema names a type JSON Schema does not define.
     *
     * @since 1.0.0
     */
    public val SCHEMA_TYPE: ViolationCode = code("schema.type")

    /**
     * A schema keyword is written in the form of another version of OpenAPI.
     *
     * @since 1.0.0
     */
    public val SCHEMA_VERSION_FORM: ViolationCode = code("schema.version-form")

    /**
     * A regular expression of a schema is invalid.
     *
     * @since 1.0.0
     */
    public val SCHEMA_PATTERN_INVALID: ViolationCode = code("schema.pattern-invalid")

    /**
     * A regular expression of a schema may backtrack catastrophically.
     *
     * @since 1.0.0
     */
    public val SCHEMA_PATTERN_BACKTRACKING: ViolationCode = ViolationCode(
        "openapi.schema.pattern-backtracking",
    )

    /**
     * A schema has a keyword its dialect does not define.
     *
     * @since 1.0.0
     */
    public val SCHEMA_UNKNOWN_KEYWORD: ViolationCode = code("schema.unknown-keyword")

    /**
     * The document breaks the official JSON Schema of its version of OpenAPI.
     *
     * @since 1.0.0
     */
    public val STRUCTURE_SCHEMA: ViolationCode = code("structure.schema")

    /**
     * A field that starts with `x-` is no valid extension name, such as one reserved by the specification.
     *
     * @since 1.0.0
     */
    public val EXTENSION_NAME: ViolationCode = code("extension.name")

    /**
     * Two tags share a name.
     *
     * @since 1.0.0
     */
    public val TAG_DUPLICATE: ViolationCode = code("tag.duplicate")

    /**
     * A link names an operation that does not exist.
     *
     * @since 1.0.0
     */
    public val LINK_TARGET: ViolationCode = code("link.target")

    /**
     * An example combines fields the specification does not allow together.
     *
     * @since 1.0.0
     */
    public val EXAMPLE_SHAPE: ViolationCode = code("example.shape")

    /**
     * A key of a content map is no valid media type or media type range.
     *
     * @since 1.0.0
     */
    public val MEDIA_TYPE_INVALID: ViolationCode = code("media-type.invalid")

    /**
     * A key of the components holds characters the specification does not allow.
     *
     * @since 1.0.0
     */
    public val COMPONENT_NAME: ViolationCode = code("component.name")

    /**
     * A key of additionalOperations of a path item is no HTTP method token as RFC 9110 defines it, which is one or more
     * letters, digits and the special characters a token allows, such as `-` and `.`.
     *
     * @since 1.0.0
     */
    public val PATH_METHOD_TOKEN: ViolationCode = code("path.method-token")

    /**
     * A key of additionalOperations of a path item names a method that has a field of its own, such as GET or get,
     * which the path item declares by that field.
     *
     * @since 1.0.0
     */
    public val PATH_METHOD_FIXED: ViolationCode = code("path.method-fixed")

    /**
     * A parameter in querystring has a schema, a style, explode or allowReserved, or a content without exactly one
     * media type; the whole query string is described by the one media type of its content.
     *
     * @since 1.0.0
     */
    public val PARAMETER_QUERYSTRING: ViolationCode = code("parameter.querystring")

    /**
     * An operation has more than one parameter in querystring, counting those of its path item.
     *
     * @since 1.0.0
     */
    public val PARAMETER_QUERYSTRING_REPEATED: ViolationCode = code("parameter.querystring-repeated")

    /**
     * An operation has a parameter in querystring and a parameter in query, counting those of its path item; the query
     * string is described either whole or by its parameters.
     *
     * @since 1.0.0
     */
    public val PARAMETER_QUERYSTRING_WITH_QUERY: ViolationCode = code("parameter.querystring-with-query")

    /**
     * The parent of a tag names no tag of the document.
     *
     * @since 1.0.0
     */
    public val TAG_PARENT_UNKNOWN: ViolationCode = code("tag.parent-unknown")

    /**
     * The parents of a tag lead back to the tag, so the tags form no hierarchy.
     *
     * @since 1.0.0
     */
    public val TAG_PARENT_CYCLE: ViolationCode = code("tag.parent-cycle")

    /**
     * A media type or an encoding has encoding beside prefixEncoding or itemEncoding; the properties of a part are
     * encoded by name or its items by position, never both.
     *
     * @since 1.0.0
     */
    public val MEDIA_TYPE_ENCODING: ViolationCode = code("media-type.encoding")

    /**
     * The nodeType of the XML object of a schema is none of element, attribute, text, cdata and none.
     *
     * @since 1.0.0
     */
    public val SCHEMA_XML_NODE_TYPE: ViolationCode = code("schema.xml-node-type")

    /**
     * The XML object of a schema has attribute or wrapped beside nodeType; OpenAPI 3.2 deprecates both in favour of
     * nodeType and forbids them beside it. A warning by default.
     *
     * @since 1.0.0
     */
    public val SCHEMA_XML_DEPRECATED: ViolationCode = code("schema.xml-deprecated")

    /**
     * The $self of a document is no URI reference without a fragment, or no absolute URI follows from it and the
     * address the document was read from; its references then resolve against that address.
     *
     * @since 1.0.0
     */
    public val DOCUMENT_SELF: ViolationCode = code("document.self")

    /**
     * Every code of an OpenAPI reading.
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
        VERSION_MISSING,
        VERSION_SWAGGER,
        VERSION_UNSUPPORTED,
        VERSION_PATCH_MISSING,
        VERSION_UPGRADED,
        UPGRADE_LOSSY,
        OVERLAY_VERSION,
        OVERLAY_SHAPE,
        OVERLAY_TARGET,
        OVERLAY_ACTION,
        OVERLAY_UNMATCHED,
        OVERLAY_UPDATE,
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
        REFERENCE_REBASED,
        LOAD_MISSING,
        LOAD_SIZE,
        LOAD_DOCUMENTS,
        LOAD_REMOTE,
        LOAD_SCHEME,
        LOAD_FAILED,
        LIMIT_NODES,
        LIMIT_DEPTH,
        LIMIT_DIAGNOSTICS,
        LIMIT_PATTERN_LENGTH,
        OPERATION_ID_DUPLICATE,
        OPERATION_RESPONSES_EMPTY,
        RESPONSE_STATUS,
        PATH_NOT_ROOTED,
        PATH_TEMPLATE,
        PATH_AMBIGUOUS,
        PATH_PARAMETER_UNDECLARED,
        PATH_PARAMETER_UNUSED,
        PARAMETER_LOCATION,
        PARAMETER_STYLE,
        PARAMETER_SHAPE,
        PARAMETER_DUPLICATE,
        PARAMETER_PATH_OPTIONAL,
        SERVER_VARIABLE_UNDECLARED,
        SERVER_VARIABLE_DEFAULT,
        SECURITY_TYPE,
        SECURITY_LOCATION,
        SECURITY_UNKNOWN_SCHEME,
        SECURITY_SCOPES,
        SCHEMA_DISCRIMINATOR_MAPPING,
        SCHEMA_CONSTRAINT,
        SCHEMA_TYPE,
        SCHEMA_VERSION_FORM,
        SCHEMA_PATTERN_INVALID,
        SCHEMA_PATTERN_BACKTRACKING,
        SCHEMA_UNKNOWN_KEYWORD,
        STRUCTURE_SCHEMA,
        EXTENSION_NAME,
        TAG_DUPLICATE,
        LINK_TARGET,
        EXAMPLE_SHAPE,
        MEDIA_TYPE_INVALID,
        COMPONENT_NAME,
        PATH_METHOD_TOKEN,
        PATH_METHOD_FIXED,
        PARAMETER_QUERYSTRING,
        PARAMETER_QUERYSTRING_REPEATED,
        PARAMETER_QUERYSTRING_WITH_QUERY,
        TAG_PARENT_UNKNOWN,
        TAG_PARENT_CYCLE,
        MEDIA_TYPE_ENCODING,
        SCHEMA_XML_NODE_TYPE,
        SCHEMA_XML_DEPRECATED,
        DOCUMENT_SELF,
    )

    private fun code(name: String): ViolationCode = ViolationCode("openapi.$name")
}
