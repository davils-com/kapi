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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.naming.Identifiers
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec

private const val SUFFIX: String = "_PATTERN"

internal class PatternTable {
    private val names: MutableMap<String, String> = linkedMapOf()

    val properties: List<PropertySpec>
        get() = names.map { (pattern, name) ->
            val property = PropertySpec.builder(name, Kotlin.pattern, KModifier.PRIVATE)
            property.initializer("%T.compileOrThrow(%S)", Kotlin.pattern, pattern)
            property.build()
        }

    fun name(subject: Subject, pattern: String): String =
        names.getOrPut(pattern) { free(Identifiers.constant(subject.property) + subject.depthMark("_")) }

    private fun free(base: String): String {
        val taken = names.values.toSet()
        val candidates = generateSequence(1) { index -> index + 1 }.map { index ->
            base + "_$index".takeIf { index > 1 }.orEmpty() + SUFFIX
        }
        return candidates.first { candidate -> candidate !in taken }
    }
}
