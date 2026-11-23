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

public object DiagnosticCodes {
    public val SOURCE_ENCODING: ViolationCode = code("source.encoding")

    public val SOURCE_MULTIPLE_DOCUMENTS: ViolationCode = ViolationCode(
        "openapi.source.multiple-documents",
    )

    public val SOURCE_KEY_NOT_SCALAR: ViolationCode = code("source.key-not-scalar")

    public val SOURCE_NON_JSON_VALUE: ViolationCode = code("source.non-json-value")

    public val SOURCE_MERGE_KEY: ViolationCode = code("source.merge-key")

    public val DOCUMENT_NOT_MAPPING: ViolationCode = code("document.not-mapping")

    public val VERSION_MISSING: ViolationCode = code("version.missing")

    public val VERSION_SWAGGER: ViolationCode = code("version.swagger")

    public val VERSION_UNSUPPORTED: ViolationCode = code("version.unsupported")

    public val VERSION_PATCH_MISSING: ViolationCode = code("version.patch-missing")

    public val VERSION_UPGRADED: ViolationCode = code("version.upgraded")

    public val UPGRADE_LOSSY: ViolationCode = code("upgrade.lossy")

    public val OVERLAY_VERSION: ViolationCode = code("overlay.version")

    public val OVERLAY_SHAPE: ViolationCode = code("overlay.shape")

    public val OVERLAY_TARGET: ViolationCode = code("overlay.target")

    public val OVERLAY_ACTION: ViolationCode = code("overlay.action")

    public val OVERLAY_UNMATCHED: ViolationCode = code("overlay.unmatched")

    public val OVERLAY_UPDATE: ViolationCode = code("overlay.update")

    public val FIELD_MISSING: ViolationCode = code("field.missing")

    public val FIELD_TYPE: ViolationCode = code("field.type")

    public val FIELD_UNKNOWN: ViolationCode = code("field.unknown")

    public val FIELD_VERSION: ViolationCode = code("field.version")

    public val REFERENCE_UNRESOLVED: ViolationCode = code("reference.unresolved")

    public val REFERENCE_CYCLE: ViolationCode = code("reference.cycle")

    public val REFERENCE_CHAIN_TOO_LONG: ViolationCode = code("reference.chain-too-long")

    public val REFERENCE_SIBLING: ViolationCode = code("reference.sibling")

    public val REFERENCE_POINTER: ViolationCode = code("reference.pointer")

    public val REFERENCE_SYNTAX: ViolationCode = code("reference.syntax")

    public val REFERENCE_NO_BASE: ViolationCode = code("reference.no-base")

    public val REFERENCE_OUTSIDE: ViolationCode = code("reference.outside")

    public val REFERENCE_KIND: ViolationCode = code("reference.kind")

    public val REFERENCE_REBASED: ViolationCode = code("reference.rebased")

    public val LOAD_MISSING: ViolationCode = code("load.missing")

    public val LOAD_SIZE: ViolationCode = code("load.size")

    public val LOAD_DOCUMENTS: ViolationCode = code("load.documents")

    public val LOAD_REMOTE: ViolationCode = code("load.remote")

    public val LOAD_SCHEME: ViolationCode = code("load.scheme")

    public val LOAD_FAILED: ViolationCode = code("load.failed")

    public val LIMIT_NODES: ViolationCode = code("limit.nodes")

    public val LIMIT_DEPTH: ViolationCode = code("limit.depth")

    public val LIMIT_DIAGNOSTICS: ViolationCode = code("limit.diagnostics")

    public val LIMIT_PATTERN_LENGTH: ViolationCode = code("limit.pattern-length")

    public val OPERATION_ID_DUPLICATE: ViolationCode = code("operation.id-duplicate")

    public val OPERATION_RESPONSES_EMPTY: ViolationCode = ViolationCode(
        "openapi.operation.responses-empty",
    )

    public val RESPONSE_STATUS: ViolationCode = code("response.status")

    public val PATH_NOT_ROOTED: ViolationCode = code("path.not-rooted")

    public val PATH_TEMPLATE: ViolationCode = code("path.template")

    public val PATH_AMBIGUOUS: ViolationCode = code("path.ambiguous")

    public val PATH_PARAMETER_UNDECLARED: ViolationCode = ViolationCode(
        "openapi.path.parameter-undeclared",
    )

    public val PATH_PARAMETER_UNUSED: ViolationCode = code("path.parameter-unused")

    public val PARAMETER_LOCATION: ViolationCode = code("parameter.location")

    public val PARAMETER_STYLE: ViolationCode = code("parameter.style")

    public val PARAMETER_SHAPE: ViolationCode = code("parameter.shape")

    public val PARAMETER_DUPLICATE: ViolationCode = code("parameter.duplicate")

    public val PARAMETER_PATH_OPTIONAL: ViolationCode = code("parameter.path-optional")

    public val SERVER_VARIABLE_UNDECLARED: ViolationCode = ViolationCode(
        "openapi.server.variable-undeclared",
    )

    public val SERVER_VARIABLE_DEFAULT: ViolationCode = code("server.variable-default")

    public val SECURITY_TYPE: ViolationCode = code("security.type")

    public val SECURITY_LOCATION: ViolationCode = code("security.location")

    public val SECURITY_UNKNOWN_SCHEME: ViolationCode = code("security.unknown-scheme")

    public val SECURITY_SCOPES: ViolationCode = code("security.scopes")

    public val SCHEMA_DISCRIMINATOR_MAPPING: ViolationCode = ViolationCode(
        "openapi.schema.discriminator-mapping",
    )

    public val SCHEMA_CONSTRAINT: ViolationCode = code("schema.constraint")

    public val SCHEMA_TYPE: ViolationCode = code("schema.type")

    public val SCHEMA_VERSION_FORM: ViolationCode = code("schema.version-form")

    public val SCHEMA_PATTERN_INVALID: ViolationCode = code("schema.pattern-invalid")

    public val SCHEMA_PATTERN_BACKTRACKING: ViolationCode = ViolationCode(
        "openapi.schema.pattern-backtracking",
    )

    public val SCHEMA_UNKNOWN_KEYWORD: ViolationCode = code("schema.unknown-keyword")

    public val STRUCTURE_SCHEMA: ViolationCode = code("structure.schema")

    public val EXTENSION_NAME: ViolationCode = code("extension.name")

    public val TAG_DUPLICATE: ViolationCode = code("tag.duplicate")

    public val LINK_TARGET: ViolationCode = code("link.target")

    public val EXAMPLE_SHAPE: ViolationCode = code("example.shape")

    public val MEDIA_TYPE_INVALID: ViolationCode = code("media-type.invalid")

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
    )

    private fun code(name: String): ViolationCode = ViolationCode("openapi.$name")
}
