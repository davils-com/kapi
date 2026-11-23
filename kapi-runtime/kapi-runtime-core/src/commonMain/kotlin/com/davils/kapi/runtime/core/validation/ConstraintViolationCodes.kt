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

package com.davils.kapi.runtime.core.validation

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations generated models report for the constraints of JSON Schema, each named
 * `kapi.runtime.constraint.<keyword>`.
 *
 * The codes are stable API: they never change their meaning once released.
 *
 * @since 1.0.0
 */
public object ConstraintViolationCodes {
    /**
     * A text is shorter than `minLength`.
     *
     * @since 1.0.0
     */
    public val MIN_LENGTH: ViolationCode = code("min-length")

    /**
     * A text is longer than `maxLength`.
     *
     * @since 1.0.0
     */
    public val MAX_LENGTH: ViolationCode = code("max-length")

    /**
     * A text does not match `pattern`.
     *
     * @since 1.0.0
     */
    public val PATTERN: ViolationCode = code("pattern")

    /**
     * A number lies below `minimum` or `exclusiveMinimum`.
     *
     * @since 1.0.0
     */
    public val MINIMUM: ViolationCode = code("minimum")

    /**
     * A number lies above `maximum` or `exclusiveMaximum`.
     *
     * @since 1.0.0
     */
    public val MAXIMUM: ViolationCode = code("maximum")

    /**
     * A number is no multiple of `multipleOf`.
     *
     * @since 1.0.0
     */
    public val MULTIPLE_OF: ViolationCode = code("multiple-of")

    /**
     * An array has fewer items than `minItems`.
     *
     * @since 1.0.0
     */
    public val MIN_ITEMS: ViolationCode = code("min-items")

    /**
     * An array has more items than `maxItems`.
     *
     * @since 1.0.0
     */
    public val MAX_ITEMS: ViolationCode = code("max-items")

    /**
     * An array repeats an item against `uniqueItems`.
     *
     * @since 1.0.0
     */
    public val UNIQUE_ITEMS: ViolationCode = code("unique-items")

    /**
     * An object has fewer properties than `minProperties`.
     *
     * @since 1.0.0
     */
    public val MIN_PROPERTIES: ViolationCode = code("min-properties")

    /**
     * An object has more properties than `maxProperties`.
     *
     * @since 1.0.0
     */
    public val MAX_PROPERTIES: ViolationCode = code("max-properties")

    /**
     * A value is none of `enum`.
     *
     * @since 1.0.0
     */
    public val ENUM: ViolationCode = code("enum")

    /**
     * A value differs from `const`.
     *
     * @since 1.0.0
     */
    public val CONST: ViolationCode = code("const")

    /**
     * A property that `dependentRequired` demands is missing.
     *
     * @since 1.0.0
     */
    public val DEPENDENT_REQUIRED: ViolationCode = code("dependent-required")

    /**
     * A condition of `if`, `then` and `else` does not hold.
     *
     * @since 1.0.0
     */
    public val CONDITION: ViolationCode = code("condition")

    /**
     * A value that must not be `null` is.
     *
     * @since 1.0.0
     */
    public val NOT_NULL: ViolationCode = code("not-null")

    /**
     * Every code of this object.
     *
     * @since 1.0.0
     */
    public val all: List<ViolationCode> = listOf(
        MIN_LENGTH,
        MAX_LENGTH,
        PATTERN,
        MINIMUM,
        MAXIMUM,
        MULTIPLE_OF,
        MIN_ITEMS,
        MAX_ITEMS,
        UNIQUE_ITEMS,
        MIN_PROPERTIES,
        MAX_PROPERTIES,
        ENUM,
        CONST,
        DEPENDENT_REQUIRED,
        CONDITION,
        NOT_NULL,
    )

    private fun code(name: String): ViolationCode = ViolationCode("kapi.runtime.constraint.$name")
}
