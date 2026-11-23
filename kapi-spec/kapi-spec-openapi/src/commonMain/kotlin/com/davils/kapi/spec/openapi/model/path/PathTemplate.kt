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

package com.davils.kapi.spec.openapi.model.path

import kotlin.jvm.JvmInline

private const val ROOT: String = "/"

private val parameterPattern: Regex = Regex("""\{([^{}/]+)}""")

@JvmInline
public value class PathTemplate(public val value: String) {
    init {
        require(value.startsWith(ROOT)) { "'$value' is no path template such as /pets/{petId}" }
    }

    public val parameters: List<String>
        get() {
            val matches = parameterPattern.findAll(value)
            return matches.map { match -> match.groupValues[1] }.toList()
        }

    override fun toString(): String = value

    public companion object {
        public fun parseOrNull(text: String): PathTemplate? {
            if (!text.startsWith(ROOT)) return null
            return PathTemplate(text)
        }
    }
}
