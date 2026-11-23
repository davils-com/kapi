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

package com.davils.kapi.runtime.core.codec

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val NULL: String = "null"

/**
 * The result of decoding content: the value it holds, or why it holds none.
 *
 * @param T the type of the value.
 * @since 1.0.0
 */
public sealed interface Decoding<out T> {
    /**
     * The content holds [value].
     *
     * @param T the type of the value.
     * @property value the decoded value.
     * @since 1.0.0
     */
    public class Decoded<out T>(public val value: T) : Decoding<T> {
        override fun equals(other: Any?): Boolean = other is Decoded<*> && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = "Decoded(${SafeText.text(value?.toString() ?: NULL)})"
    }

    /**
     * The content holds no value of the expected kind.
     *
     * The reason may quote the content, which may come from an attacker: it is shown escaped by [toString], and
     * whatever else shows it escapes it at its own sink.
     *
     * @property reason why the content was refused.
     * @since 1.0.0
     */
    public class Failed(public val reason: String) : Decoding<Nothing> {
        override fun equals(other: Any?): Boolean = other is Failed && reason == other.reason

        override fun hashCode(): Int = reason.hashCode()

        override fun toString(): String = "Failed(${SafeText.text(reason)})"
    }
}
