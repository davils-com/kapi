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

import com.squareup.kotlinpoet.CodeBlock

internal class Subject(val value: CodeBlock, val property: String, val depth: Int) {
    val isProperty: Boolean get() = depth == 0

    val target: CodeBlock get() = CodeBlock.of("::%N, ", property).takeIf { isProperty } ?: CodeBlock.of("")

    val itemName: String get() = "item" + depthMark("")

    fun item(): Subject = Subject(CodeBlock.of("%N", itemName), property, depth + 1)

    fun depthMark(prefix: String): String {
        if (isProperty) return ""
        return prefix + depth
    }
}
