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

package com.davils.kapi.spec.schema.internal.value

internal object Texts {
    fun codePoints(text: String): Int {
        var count = 0
        var index = 0
        while (index < text.length) {
            val isPair = text[index].isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()
            index += 2.takeIf { isPair } ?: 1
            count++
        }
        return count
    }

    fun escapedToken(token: String): String {
        val tildes = token.replace(oldValue = "~", newValue = "~0")
        return tildes.replace(oldValue = "/", newValue = "~1")
    }
}
