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

import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.model.plan.PropertyPlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.squareup.kotlinpoet.CodeBlock

private const val NOT_NULL: String = "must not be null"

internal class ValidationCode(patterns: PatternTable) {
    private val rules: RuleCode = RuleCode(patterns)

    fun property(property: PropertyPlan): CodeBlock {
        if (property.isPatch) return patch(property)
        return checks(property.type, Subject(CodeBlock.of("%N", property.name), property.name, depth = 0))
    }

    private fun patch(property: PropertyPlan): CodeBlock {
        val value = Subject(CodeBlock.of("%N.value", property.name), property.name, depth = 0)
        val inner = checks(property.type.copy(isNullable = false), value)
        val code = CodeBlock.builder()
        if (!property.type.isNullable) {
            val cleared = CodeBlock.of("%N is %T.Null", property.name, Kotlin.patch)
            code.add("if (%L) fail(::%N, %S, %T.NOT_NULL)\n", cleared, property.name, NOT_NULL, Kotlin.codes)
        }
        if (inner.isNotEmpty()) {
            code.beginControlFlow("if (%N is %T.Value)", property.name, Kotlin.patch)
            code.add(inner)
            code.endControlFlow()
        }
        return code.build()
    }

    private fun checks(use: TypeUse, subject: Subject): CodeBlock {
        val scalar = use.shape as? TypeShape.Scalar
        val kind = scalar?.kind ?: ScalarKind.Json
        val body = CodeBlock.builder()
        use.rules.forEach { rule -> body.add(rules.of(rule, kind, subject)) }
        body.add(nested(use.shape, subject))
        val block = body.build()
        val isGuarded = use.isNullable && block.isNotEmpty()
        if (!isGuarded) return block
        val guard = CodeBlock.builder()
        guard.beginControlFlow("if (%L != null)", subject.value)
        guard.add(block)
        guard.endControlFlow()
        return guard.build()
    }

    private fun nested(shape: TypeShape, subject: Subject): CodeBlock = when (shape) {
        is TypeShape.Named -> named(shape, subject)
        is TypeShape.Items -> each(checks(shape.item, subject.item()), subject, subject.value)
        is TypeShape.Entries -> each(checks(shape.value, subject.item()), subject, entriesOf(subject))
        is TypeShape.Scalar, is TypeShape.External -> CodeBlock.of("")
    }

    private fun entriesOf(subject: Subject): CodeBlock =
        subject.value.takeIf { subject.isProperty } ?: CodeBlock.of("%L.values", subject.value)

    private fun named(shape: TypeShape.Named, subject: Subject): CodeBlock {
        if (shape.kind == ModelKind.Enum) {
            return rules.failure(
                condition = CodeBlock.of("!%L.isKnown", subject.value),
                subject = subject,
                message = "must be one of the values this API knows",
                code = "ENUM",
            )
        }
        val validate = CodeBlock.of("with(%L) { validate() }", subject.value)
        val within = CodeBlock.of("within(::%N) { %L }\n", subject.property, validate)
        return within.takeIf { subject.isProperty } ?: CodeBlock.of("%L\n", validate)
    }

    private fun each(inner: CodeBlock, subject: Subject, items: CodeBlock): CodeBlock {
        if (inner.isEmpty()) return inner
        val open = CodeBlock.of("each(::%N, %L) { %N ->", subject.property, items, subject.itemName)
        val loop = open.takeIf { subject.isProperty } ?: CodeBlock.of("%L.forEach { %N ->", items, subject.itemName)
        val block = CodeBlock.builder()
        block.add("%L\n", loop)
        block.indent()
        block.add(inner)
        block.unindent()
        block.add("}\n")
        return block.build()
    }
}
