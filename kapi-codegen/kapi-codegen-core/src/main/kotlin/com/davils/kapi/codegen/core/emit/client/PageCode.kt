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

import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.PagePlan
import com.davils.kapi.codegen.core.operation.plan.PageStyle
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.joinToCode

private const val PAGES: String = "com.davils.kapi.runtime.client.page"

private val flow: ClassName = ClassName("kotlinx.coroutines.flow", "Flow")

private val readers: Map<PageStyle, String> = mapOf(
    PageStyle.Cursor to "next",
    PageStyle.Offset to "size",
    PageStyle.Numbered to "size",
)

internal class PageCode(private val signatures: SignatureCode, private val visibility: KModifier) {
    fun function(operation: OperationPlan, pages: PagePlan, result: ClassName): FunSpec {
        val taken = signatures.taken(operation).filter { (name, _) -> name != pages.parameter }
        val function = FunSpec.builder(operation.name + SUFFIX)
        function.addModifiers(visibility)
        taken.forEach { (name, type) -> function.addParameter(signatures.parameter(name, type)) }
        function.returns(flow.parameterizedBy(result))
        val pager = MemberName(PAGES, pages.style.function)
        function.addCode("return %M(%L = { %L }) {\n", pager, readers.getValue(pages.style), read(pages, result))
        function.addCode("⇥%N(%L)\n", operation.name, arguments(pages, taken.map { (name, _) -> name }))
        function.addCode("⇤}\n")
        return function.build()
    }

    private fun read(pages: PagePlan, result: ClassName): CodeBlock {
        val body = CodeBlock.of("(it as? %T.%L)?.body", result, pages.answer)
        val paged = pages.property?.let { property -> CodeBlock.of("%L?.%N", body, property) } ?: body
        if (pages.style == PageStyle.Cursor) return paged
        return CodeBlock.of("%L?.size", paged)
    }

    private fun arguments(pages: PagePlan, names: List<String>): CodeBlock {
        val position = CodeBlock.of("it.toInt()").takeIf { pages.isNarrow } ?: CodeBlock.of("it")
        val kept = names.map { name -> CodeBlock.of("%N = %N", name, name) }
        return (kept + CodeBlock.of("%N = %L", pages.parameter, position)).joinToCode(", ")
    }

    companion object {
        const val SUFFIX: String = "Pages"
    }
}
