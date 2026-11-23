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

import kotlin.jvm.JvmInline

private const val PREFIX: String = "x-"

@JvmInline
public value class ExtensionName(public val value: String) {
    init {
        require(isExtension(value)) { "'$value' is no extension name such as x-kapi-name" }
    }

    override fun toString(): String = value

    public companion object {
        public fun isExtension(text: String): Boolean = text.startsWith(PREFIX) && text.length > PREFIX.length

        public fun parseOrNull(text: String): ExtensionName? {
            if (!isExtension(text)) return null
            return ExtensionName(text)
        }
    }
}
