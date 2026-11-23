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

public object CodegenCodes {
    public val EXTENSION_UNKNOWN: ViolationCode = code("extension.unknown")

    public val EXTENSION_SITE: ViolationCode = code("extension.site")

    public val EXTENSION_VALUE: ViolationCode = code("extension.value")

    public val NAME_COLLISION: ViolationCode = code("name.collision")

    public val SCHEMA_UNSUPPORTED: ViolationCode = code("schema.unsupported")

    public val SCHEMA_UNCHECKED: ViolationCode = code("schema.unchecked")

    public val REFERENCE_UNSUPPORTED: ViolationCode = code("reference.unsupported")

    public val ENUM_VALUES: ViolationCode = code("enum.values")

    public val PROPERTY_IGNORED: ViolationCode = code("property.ignored")

    public val OPERATION_UNSUPPORTED: ViolationCode = code("operation.unsupported")

    public val EXTENSION_PENDING: ViolationCode = code("extension.pending")

    public val PARAMETER_RESERVED_HEADER: ViolationCode = code("parameter.reserved-header")

    public val PAGINATION_INVALID: ViolationCode = code("pagination.invalid")

    public val SAMPLE_UNAVAILABLE: ViolationCode = code("sample.unavailable")

    public val MOCK_UNANSWERED: ViolationCode = code("mock.unanswered")

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
    )

    private fun code(name: String): ViolationCode = ViolationCode("codegen.$name")
}
