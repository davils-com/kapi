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

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

private const val PARAGRAPH: String = "\n\n"

internal object Docs {
    fun of(vararg texts: String?): String? {
        val shown = texts.mapNotNull { text -> text?.let { found -> Texts.kdoc(found).takeIf(String::isNotEmpty) } }
        return shown.joinToString(separator = PARAGRAPH).ifEmpty { null }
    }

    fun on(type: TypeSpec.Builder, vararg texts: String?) {
        of(*texts)?.let { found -> type.addKdoc("%L", found) }
    }

    fun on(function: FunSpec.Builder, vararg texts: String?) {
        of(*texts)?.let { found -> function.addKdoc("%L", found) }
    }

    fun on(property: PropertySpec.Builder, vararg texts: String?) {
        of(*texts)?.let { found -> property.addKdoc("%L", found) }
    }
}
