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

package com.davils.kapi.spec.core.value

/**
 * The kinds of values a document can hold, the six kinds of JSON.
 *
 * @since 1.0.0
 */
public enum class ValueKind {
    /**
     * `null`.
     *
     * @since 1.0.0
     */
    Null,

    /**
     * `true` or `false`.
     *
     * @since 1.0.0
     */
    Flag,

    /**
     * A number.
     *
     * @since 1.0.0
     */
    Number,

    /**
     * A string.
     *
     * @since 1.0.0
     */
    Text,

    /**
     * An array.
     *
     * @since 1.0.0
     */
    Sequence,

    /**
     * An object.
     *
     * @since 1.0.0
     */
    Mapping,
}
