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

package com.davils.kapi.runtime.core.codec.form

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures [FormLimits]; every limit starts at its default.
 *
 * A builder is not thread-safe; the limits it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class FormLimitsBuilder internal constructor() : ValidatingBuilder<FormLimits>() {
    override val subject: String get() = "form limits"

    /**
     * The most characters of a whole body.
     *
     * @since 1.0.0
     */
    public var maxLength: Int = DEFAULT_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The most fields of a body.
     *
     * @since 1.0.0
     */
    public var maxFields: Int = DEFAULT_FIELDS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The most characters of the encoded name or value of one field.
     *
     * @since 1.0.0
     */
    public var maxFieldLength: Int = DEFAULT_FIELD_LENGTH
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): FormLimits = FormLimits(this)

    private companion object {
        const val DEFAULT_LENGTH: Int = 1_024 * 1_024
        const val DEFAULT_FIELDS: Int = 1_000
        const val DEFAULT_FIELD_LENGTH: Int = 64 * 1_024
    }
}

/**
 * Builds the limits of a reader of `application/x-www-form-urlencoded`.
 *
 * @param configure changes the limits from their defaults.
 * @return the limits.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit is not positive or lies above its
 * hard ceiling.
 * @since 1.0.0
 */
public fun formLimits(configure: FormLimitsBuilder.() -> Unit = {}): FormLimits {
    val builder = FormLimitsBuilder()
    builder.configure()
    return builder.build()
}
