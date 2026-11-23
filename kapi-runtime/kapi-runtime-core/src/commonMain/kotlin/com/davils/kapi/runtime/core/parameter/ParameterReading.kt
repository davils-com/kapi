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

package com.davils.kapi.runtime.core.parameter

/**
 * The result of reading a parameter from a request.
 *
 * @since 1.0.0
 */
public sealed interface ParameterReading {
    /**
     * The request does not carry the parameter.
     *
     * @since 1.0.0
     */
    public object Absent : ParameterReading {
        override fun toString(): String = "Absent"
    }

    /**
     * The request carries the parameter with [value].
     *
     * @property value the value.
     * @since 1.0.0
     */
    public class Present(public val value: ParameterValue) : ParameterReading {
        override fun equals(other: Any?): Boolean = other is Present && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = "Present($value)"
    }

    /**
     * The request carries the parameter in a form its style does not allow, such as a key without a value, a name
     * repeated where one value is expected, or more parts than the runtime reads.
     *
     * @since 1.0.0
     */
    public object Malformed : ParameterReading {
        override fun toString(): String = "Malformed"
    }
}
