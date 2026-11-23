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

package com.davils.kapi.spec.schema

/**
 * Why a [Validation] was refused instead of completed.
 *
 * A refusal is no verdict on the value: the validation stopped at a limit or at a value it cannot read, and the value
 * counts as not valid. Every limit is configured in [SchemaLimits].
 *
 * @since 1.0.0
 */
public enum class RefusalReason {
    /**
     * The validation needed more than [SchemaLimits.maximumEvaluationSteps] steps.
     *
     * @since 1.0.0
     */
    EvaluationSteps,

    /**
     * The validation nested subschemas deeper than [SchemaLimits.maximumEvaluationDepth].
     *
     * @since 1.0.0
     */
    EvaluationDepth,

    /**
     * The value is nested deeper than [SchemaLimits.maximumInstanceDepth].
     *
     * @since 1.0.0
     */
    InstanceDepth,

    /**
     * The value has more nodes than [SchemaLimits.maximumInstanceNodes].
     *
     * @since 1.0.0
     */
    InstanceSize,

    /**
     * The value has more errors than [SchemaLimits.maximumErrors].
     *
     * @since 1.0.0
     */
    ReportedErrors,

    /**
     * A string that a `pattern`, a `patternProperties`, an `additionalProperties` with patterns or an asserted
     * `format` reads is longer than [SchemaLimits.maximumStringLength].
     *
     * @since 1.0.0
     */
    StringLength,

    /**
     * A chain of references leads back to a subschema it already applies to the same part of the value, which would
     * never end.
     *
     * @since 1.0.0
     */
    ReferenceCycle,

    /**
     * The value holds something that is no JSON value or that kapi does not read: a number of more than 4096
     * characters or with an exponent of more than 15 digits, or a YAML node that JSON cannot express.
     *
     * @since 1.0.0
     */
    UnsupportedValue,
}
