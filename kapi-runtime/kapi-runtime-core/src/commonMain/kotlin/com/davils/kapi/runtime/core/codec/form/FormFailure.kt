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

/**
 * Why a body of `application/x-www-form-urlencoded` was refused.
 *
 * @since 1.0.0
 */
public enum class FormFailure {
    /**
     * The body is longer than [FormLimits.maxLength].
     *
     * @since 1.0.0
     */
    TooLong,

    /**
     * The body has more fields than [FormLimits.maxFields].
     *
     * @since 1.0.0
     */
    TooManyFields,

    /**
     * The encoded name or value of a field is longer than [FormLimits.maxFieldLength].
     *
     * @since 1.0.0
     */
    FieldTooLong,

    /**
     * A percent sign does not start a percent-encoded octet.
     *
     * @since 1.0.0
     */
    MalformedEscape,

    /**
     * The decoded octets of a name or a value are no UTF-8.
     *
     * @since 1.0.0
     */
    NotUtf8,
}
