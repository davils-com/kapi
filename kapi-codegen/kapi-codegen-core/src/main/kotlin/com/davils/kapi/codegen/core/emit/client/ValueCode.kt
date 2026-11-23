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

package com.davils.kapi.codegen.core.emit.client

import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.squareup.kotlinpoet.CodeBlock

internal object ValueCode {
    fun text(shape: TypeShape, name: String): CodeBlock {
        if (isText(shape)) return CodeBlock.of("%N", name)
        return CodeBlock.of("%N.toString()", name)
    }

    fun texts(items: TypeShape.Items, name: String): CodeBlock {
        if (isText(items.item.shape)) return CodeBlock.of("%N", name)
        return CodeBlock.of("%N.map { it.toString() }", name)
    }

    private fun isText(shape: TypeShape): Boolean = (shape as? TypeShape.Scalar)?.kind == ScalarKind.Text
}
