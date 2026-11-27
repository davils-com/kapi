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

package com.davils.kapi.codegen.core

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the diagnostics of the generator, each under `codegen.`.
 *
 * @since 1.0.0
 */
public object CodegenCodes {
    /**
     * An extension keyword under `x-kapi-` that the generator does not know.
     *
     * @since 1.0.0
     */
    public val EXTENSION_UNKNOWN: ViolationCode = code("extension.unknown")

    /**
     * An extension keyword where it does not belong, such as a keyword of a schema on an operation.
     *
     * @since 1.0.0
     */
    public val EXTENSION_SITE: ViolationCode = code("extension.site")

    /**
     * An extension keyword whose value is not what the keyword takes.
     *
     * @since 1.0.0
     */
    public val EXTENSION_VALUE: ViolationCode = code("extension.value")

    /**
     * Two things of the document that become the same Kotlin name, or a name the generated code uses itself.
     *
     * @since 1.0.0
     */
    public val NAME_COLLISION: ViolationCode = code("name.collision")

    /**
     * A schema the generator has no faithful Kotlin for, refused instead of emitting something weaker.
     *
     * @since 1.0.0
     */
    public val SCHEMA_UNSUPPORTED: ViolationCode = code("schema.unsupported")

    /**
     * A keyword of a schema that the generated code does not check, such as a format it cannot assert.
     *
     * @since 1.0.0
     */
    public val SCHEMA_UNCHECKED: ViolationCode = code("schema.unchecked")

    /**
     * A reference the generator cannot follow to a model, such as one into another document without a catalog.
     *
     * @since 1.0.0
     */
    public val REFERENCE_UNSUPPORTED: ViolationCode = code("reference.unsupported")

    /**
     * An enumeration whose values cannot become constants, such as values of mixed types.
     *
     * @since 1.0.0
     */
    public val ENUM_VALUES: ViolationCode = code("enum.values")

    /**
     * A property the generated model leaves out, because the document asks for it.
     *
     * @since 1.0.0
     */
    public val PROPERTY_IGNORED: ViolationCode = code("property.ignored")

    /**
     * An operation, parameter, body or response the generator has no faithful Kotlin for.
     *
     * @since 1.0.0
     */
    public val OPERATION_UNSUPPORTED: ViolationCode = code("operation.unsupported")

    /**
     * An extension keyword the generator reads but does not act on yet.
     *
     * @since 1.0.0
     */
    public val EXTENSION_PENDING: ViolationCode = code("extension.pending")

    /**
     * A header parameter the generated client sets itself, such as `Accept`, which gets no parameter.
     *
     * @since 1.0.0
     */
    public val PARAMETER_RESERVED_HEADER: ViolationCode = code("parameter.reserved-header")

    /**
     * An `x-kapi-pagination` that does not fit the parameters or the response of its operation.
     *
     * @since 1.0.0
     */
    public val PAGINATION_INVALID: ViolationCode = code("pagination.invalid")

    /**
     * A model for which no sample satisfies the rules of its schema.
     *
     * @since 1.0.0
     */
    public val SAMPLE_UNAVAILABLE: ViolationCode = code("sample.unavailable")

    /**
     * An operation whose mock is left to a subclass, because none of its responses has a sample.
     *
     * @since 1.0.0
     */
    public val MOCK_UNANSWERED: ViolationCode = code("mock.unanswered")

    /**
     * A body of XML whose schema has no faithful mapping to XML, such as an attribute, a wrapped array, a namespace, a
     * map or a free-form value, refused instead of written in a shape the document does not describe.
     *
     * @since 1.2.0
     */
    public val XML_UNSUPPORTED: ViolationCode = code("xml.unsupported")

    /**
     * Every code of the generator; codes are stable API.
     *
     * @since 1.0.0
     */
    public val all: List<ViolationCode> = listOf(
        EXTENSION_UNKNOWN,
        EXTENSION_SITE,
        EXTENSION_VALUE,
        NAME_COLLISION,
        SCHEMA_UNSUPPORTED,
        SCHEMA_UNCHECKED,
        REFERENCE_UNSUPPORTED,
        ENUM_VALUES,
        PROPERTY_IGNORED,
        OPERATION_UNSUPPORTED,
        EXTENSION_PENDING,
        PARAMETER_RESERVED_HEADER,
        PAGINATION_INVALID,
        SAMPLE_UNAVAILABLE,
        MOCK_UNANSWERED,
        XML_UNSUPPORTED,
    )

    private fun code(name: String): ViolationCode = ViolationCode("codegen.$name")
}
