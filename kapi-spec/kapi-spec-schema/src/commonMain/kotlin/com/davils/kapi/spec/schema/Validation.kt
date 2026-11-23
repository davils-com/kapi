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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.schema

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * The result of validating a value against a [CompiledSchema].
 *
 * A validation is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public sealed interface Validation {
    /**
     * Whether the value is valid; `false` for [Invalid] and for [Refused].
     *
     * @since 1.0.0
     */
    public val isValid: Boolean

    /**
     * The value is valid.
     *
     * @since 1.0.0
     */
    public object Valid : Validation {
        override val isValid: Boolean get() = true

        override fun toString(): String = "Valid"
    }

    /**
     * The value is not valid.
     *
     * @property errors every failing keyword in the order it was found, as the "basic" output format of JSON Schema
     * 2020-12 lists them; empty when the validation ran with [OutputFormat.Flag]. It holds at most
     * [SchemaLimits.maximumErrors] units, since a value with more is [Refused].
     * @since 1.0.0
     */
    public class Invalid internal constructor(public val errors: List<OutputUnit>) : Validation {
        override val isValid: Boolean get() = false

        override fun equals(other: Any?): Boolean = other is Invalid && errors == other.errors

        override fun hashCode(): Int = errors.hashCode()

        override fun toString(): String = "Invalid(${errors.joinToString("; ")})"
    }

    /**
     * The validation stopped before it could decide, at a limit or at a value it cannot read; the value counts as not
     * valid.
     *
     * @property reason why the validation stopped.
     * @property message what stopped it, in English.
     * @property instanceLocation the place in the value where it stopped.
     * @since 1.0.0
     */
    public class Refused internal constructor(
        public val reason: RefusalReason,
        public val message: String,
        public val instanceLocation: JsonPointer,
    ) : Validation {
        override val isValid: Boolean get() = false

        private val identity: List<Any> get() = listOf(reason, message, instanceLocation)

        override fun equals(other: Any?): Boolean = other is Refused && identity == other.identity

        override fun hashCode(): Int = identity.hashCode()

        override fun toString(): String = "Refused($reason at ${SafeText.text(instanceLocation.text)}: $message)"
    }
}
