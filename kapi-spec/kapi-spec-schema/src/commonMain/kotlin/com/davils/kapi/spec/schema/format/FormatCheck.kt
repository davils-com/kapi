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

package com.davils.kapi.spec.schema.format

/**
 * The result of [Formats.check]: the value is [Valid] or [Invalid] for its format, or the format is [Unknown].
 *
 * Every result is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public sealed interface FormatCheck {
    /**
     * The value is valid for its format.
     *
     * @since 1.0.0
     */
    public object Valid : FormatCheck {
        override fun toString(): String = "Valid"
    }

    /**
     * The value is not valid for its format.
     *
     * @property reason why the value is invalid, in words for people; it never repeats the value, so it is safe to log
     * and to show.
     * @since 1.0.0
     */
    public class Invalid(public val reason: String) : FormatCheck {
        private val identity: List<String> get() = listOf(reason)

        override fun equals(other: Any?): Boolean = other is Invalid && identity == other.identity

        override fun hashCode(): Int = identity.hashCode()

        override fun toString(): String = "Invalid(reason=$reason)"
    }

    /**
     * The format is none [Formats] knows, so the value was not checked; JSON Schema treats such a format as an
     * annotation.
     *
     * @since 1.0.0
     */
    public object Unknown : FormatCheck {
        override fun toString(): String = "Unknown"
    }
}
