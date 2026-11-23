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

package com.davils.kapi.spec.core.value

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline

private const val PREFIX: String = "x-"

/**
 * The name of a specification extension, a field that starts with `x-`, such as `x-kapi-name`.
 *
 * Its [toString] escapes the name, because a document may choose any text after the prefix.
 *
 * @property value the name as it was written.
 * @since 1.0.0
 */
@JvmInline
public value class ExtensionName(public val value: String) {
    init {
        require(isExtension(value)) { "${SafeText.quoted(value)} is no extension name such as x-kapi-name" }
    }

    override fun toString(): String = SafeText.text(value)

    /**
     * Reads extension names.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Whether [text] names an extension: it starts with `x-` and has at least one character after it.
         *
         * @param text the text, which may contain anything.
         * @return `true` if [text] is an extension name.
         * @since 1.0.0
         */
        public fun isExtension(text: String): Boolean = text.startsWith(PREFIX) && text.length > PREFIX.length

        /**
         * Reads [text] as an extension name.
         *
         * @param text the text, which may contain anything.
         * @return the extension name, or `null` if [text] is none.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): ExtensionName? {
            if (!isExtension(text)) return null
            return ExtensionName(text)
        }
    }
}
