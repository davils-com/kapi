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

package com.davils.kapi.runtime.testing.contract

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the findings of a [ContractCheck]. The codes are stable API.
 *
 * @since 1.0.0
 */
public object ContractCodes {
    /**
     * No path template of the document matches the path of the request.
     *
     * @since 1.0.0
     */
    public val OPERATION_UNKNOWN: ViolationCode = ViolationCode("kapi.testing.contract.operation-unknown")

    /**
     * A path template matches the path of the request, but its path item declares no operation for the method.
     *
     * @since 1.0.0
     */
    public val METHOD_UNDECLARED: ViolationCode = ViolationCode("kapi.testing.contract.method-undeclared")

    /**
     * A required parameter is missing from the request.
     *
     * @since 1.0.0
     */
    public val PARAMETER_MISSING: ViolationCode = ViolationCode("kapi.testing.contract.parameter-missing")

    /**
     * A parameter of the request does not validate against its schema.
     *
     * @since 1.0.0
     */
    public val PARAMETER_INVALID: ViolationCode = ViolationCode("kapi.testing.contract.parameter-invalid")

    /**
     * A required request body is missing.
     *
     * @since 1.0.0
     */
    public val BODY_MISSING: ViolationCode = ViolationCode("kapi.testing.contract.body-missing")

    /**
     * A request or response has a body where the document declares none.
     *
     * @since 1.0.0
     */
    public val BODY_UNDECLARED: ViolationCode = ViolationCode("kapi.testing.contract.body-undeclared")

    /**
     * A body of a JSON media type is no JSON.
     *
     * @since 1.0.0
     */
    public val BODY_MALFORMED: ViolationCode = ViolationCode("kapi.testing.contract.body-malformed")

    /**
     * A body does not validate against the schema of its media type.
     *
     * @since 1.0.0
     */
    public val BODY_INVALID: ViolationCode = ViolationCode("kapi.testing.contract.body-invalid")

    /**
     * A body has no `Content-Type`, or one that is no media type.
     *
     * @since 1.0.0
     */
    public val CONTENT_TYPE_MISSING: ViolationCode = ViolationCode("kapi.testing.contract.content-type-missing")

    /**
     * The `Content-Type` of a body is not among the media types the document declares for it.
     *
     * @since 1.0.0
     */
    public val CONTENT_TYPE_UNDECLARED: ViolationCode = ViolationCode("kapi.testing.contract.content-type-undeclared")

    /**
     * The status of the response is documented neither by its code, nor by its range, nor by `default`.
     *
     * @since 1.0.0
     */
    public val STATUS_UNDOCUMENTED: ViolationCode = ViolationCode("kapi.testing.contract.status-undocumented")

    /**
     * A required header of the response is missing.
     *
     * @since 1.0.0
     */
    public val HEADER_MISSING: ViolationCode = ViolationCode("kapi.testing.contract.header-missing")

    /**
     * A header of the response does not validate against its schema.
     *
     * @since 1.0.0
     */
    public val HEADER_INVALID: ViolationCode = ViolationCode("kapi.testing.contract.header-invalid")

    /**
     * A reference of the document that the check needs does not resolve.
     *
     * @since 1.0.0
     */
    public val REFERENCE_UNRESOLVED: ViolationCode = ViolationCode("kapi.testing.contract.reference-unresolved")

    /**
     * A schema of the document does not compile, so nothing can be validated against it.
     *
     * @since 1.0.0
     */
    public val SCHEMA_INVALID: ViolationCode = ViolationCode("kapi.testing.contract.schema-invalid")

    /**
     * The validation of a value stopped at a limit of the schema compiler before it reached a verdict.
     *
     * @since 1.0.0
     */
    public val VALIDATION_REFUSED: ViolationCode = ViolationCode("kapi.testing.contract.validation-refused")
}
