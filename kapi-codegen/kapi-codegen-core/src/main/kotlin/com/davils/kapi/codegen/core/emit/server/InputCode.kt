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

package com.davils.kapi.codegen.core.emit.server

import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.emit.client.SerializerCode
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.BodyPlan
import com.davils.kapi.codegen.core.operation.plan.FieldKind
import com.davils.kapi.codegen.core.operation.plan.FieldPlan
import com.davils.kapi.codegen.core.operation.plan.OperationPlan
import com.davils.kapi.codegen.core.operation.plan.ParameterPlace
import com.davils.kapi.codegen.core.operation.plan.ParameterPlan
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode

private const val BODY: String = "body"

private const val FORM: String = "form"

private const val PARTS: String = "parts"

private const val INPUT: String = "input"

private const val JSON: String = "application/json"

private const val FORM_TYPE: String = "application/x-www-form-urlencoded"

private const val TEXT: String = "text/plain"

private const val ABSENT: String = "%N == null"

private const val DECLARED: String = "val %N = %L\n"

private const val LET: String = "%L?.let { part -> %L }"

private const val MAP: String = "%L?.map { part -> %L }"

internal class InputCode(private val serializers: SerializerCode, private val fields: Map<String, List<String>>) {
    fun of(operation: OperationPlan, form: ClassName): Reading {
        val code = CodeBlock.builder()
        val checks = mutableListOf(CodeBlock.of("!%N.isValid", INPUT))
        val arguments = mutableListOf<Pair<String, CodeBlock>>()
        code.add("val %N = request.input()\n", INPUT)
        operation.parameters.forEach { parameter ->
            code.add(DECLARED, parameter.name, read(parameter))
            if (!parameter.type.isNullable) checks += CodeBlock.of(ABSENT, parameter.name)
            arguments += parameter.name to CodeBlock.of("%N", parameter.name)
        }
        val body = operation.body
        if (body != null) {
            val reading = body(body, form)
            code.add(reading.code)
            checks += reading.checks
            arguments += reading.arguments
        }
        code.add("if (%L) return %N.refused()\n", checks.joinToCode(" || "), INPUT)
        return Reading(code.build(), emptyList(), arguments)
    }

    private fun body(body: BodyPlan, form: ClassName): Reading = when (body.kind) {
        BodyKind.Json -> whole(
            body,
            CodeBlock.of("%N.body(%L, %L, %L)", INPUT, serializer(body.type), required(body), ranges(body)),
        )

        BodyKind.Binary -> whole(body, CodeBlock.of("%N.bytes(%L, %L)", INPUT, required(body), ranges(body)))

        BodyKind.Upload -> whole(body, CodeBlock.of("%N.upload(%L, %L)", INPUT, required(body), ranges(body)))

        BodyKind.Form -> form(body, form)

        BodyKind.Multipart -> multipart(body)
    }

    private fun whole(body: BodyPlan, read: CodeBlock): Reading {
        val code = CodeBlock.of(DECLARED, BODY, read)
        val checks = listOf(CodeBlock.of(ABSENT, BODY)).filter { !body.type.isNullable }
        return Reading(code, checks, listOf(BODY to CodeBlock.of("%N", BODY)))
    }

    private fun form(body: BodyPlan, form: ClassName): Reading {
        val read = CodeBlock.of(
            "%N.body(%T.serializer(), true, %L)",
            INPUT,
            form,
            ServerTypes.ranges(FORM_TYPE),
        )
        val code = CodeBlock.builder()
        code.add(DECLARED, FORM, read)
        val checks = mutableListOf(CodeBlock.of(ABSENT, FORM))
        val arguments = body.fields.map { field ->
            val isDecoded = isDecoded(field)
            if (isDecoded) {
                val content = CodeBlock.of("text.%M()", ServerTypes.encodeToByteString)
                val decoding = decoding(field, content, JSON)
                code.add("val %N = %N?.%N?.let { text -> %L }\n", field.name, FORM, field.name, decoding)
                checks += decodedChecks(field)
            }
            val value = CodeBlock.of("%N.%N", FORM, field.name)
            field.name to (decodedArgument(field).takeIf { isDecoded } ?: value)
        }
        return Reading(code.build(), checks, arguments)
    }

    private fun multipart(body: BodyPlan): Reading {
        val code = CodeBlock.builder()
        code.add("val %N = %N.parts(%L)\n", PARTS, INPUT, isRequired(body))
        val checks = mutableListOf<CodeBlock>()
        val arguments = body.fields.map { field ->
            code.add(DECLARED, field.name, part(field))
            checks += partChecks(field)
            field.name to partArgument(field)
        }
        return Reading(code.build(), checks, arguments)
    }

    private fun part(field: FieldPlan): CodeBlock {
        val named = CodeBlock.of("{ part -> part.name == %S }", field.wireName)
        val single = CodeBlock.of("%N?.firstOrNull %L", PARTS, named)
        val many = CodeBlock.of("%N?.filter %L", PARTS, named)
        val file = CodeBlock.of(
            "%T(part.content, part.filename, part.contentType?.toString())",
            ServerTypes.filePart,
        )
        val text = CodeBlock.of("part.content.%M()", ServerTypes.decodeToString)
        val content = CodeBlock.of("part.content")
        return when (field.kind) {
            FieldKind.File -> CodeBlock.of(LET, single, file)

            FieldKind.Files -> CodeBlock.of(MAP, many, file)

            FieldKind.Text -> CodeBlock.of(
                LET,
                single,
                text.takeUnless { isDecoded(field) } ?: decoding(field, content, TEXT),
            )

            FieldKind.Texts -> CodeBlock.of(MAP, many, text)

            FieldKind.Json -> CodeBlock.of(
                LET,
                single,
                decoding(field, content, field.contentType ?: JSON),
            )
        }
    }

    private fun decoding(field: FieldPlan, content: CodeBlock, mediaType: String): CodeBlock = CodeBlock.of(
        "%T.Default.decode(%L, %L, %S)",
        ServerTypes.codecs,
        serializer(field.type),
        content,
        mediaType,
    )

    private fun isDecoded(field: FieldPlan): Boolean {
        if (field.kind == FieldKind.Json) return true
        val shape = field.type.shape
        return field.kind == FieldKind.Text && (shape as? TypeShape.Scalar)?.kind != ScalarKind.Text
    }

    private fun partChecks(field: FieldPlan): List<CodeBlock> {
        if (isDecoded(field)) return decodedChecks(field)
        return listOf(CodeBlock.of(ABSENT, field.name)).filter { !field.type.isNullable }
    }

    private fun partArgument(field: FieldPlan): CodeBlock {
        if (isDecoded(field)) return decodedArgument(field)
        return CodeBlock.of("%N", field.name)
    }

    private fun decodedChecks(field: FieldPlan): List<CodeBlock> {
        if (!field.type.isNullable) return listOf(CodeBlock.of("%N !is %T", field.name, ServerTypes.decoded))
        return listOf(CodeBlock.of("(%N != null && %N !is %T)", field.name, field.name, ServerTypes.decoded))
    }

    private fun decodedArgument(field: FieldPlan): CodeBlock {
        if (!field.type.isNullable) return CodeBlock.of("%N.value", field.name)
        return CodeBlock.of("%N?.value", field.name)
    }

    private fun isRequired(body: BodyPlan): Boolean =
        !body.type.isNullable || body.fields.any { field -> !field.type.isNullable }

    private fun required(body: BodyPlan): Boolean = !body.type.isNullable

    private fun ranges(body: BodyPlan): CodeBlock = ServerTypes.ranges(body.contentType)

    private fun serializer(type: TypeUse): CodeBlock = serializers.of(type.copy(isNullable = false))

    private fun read(parameter: ParameterPlan): CodeBlock {
        val shape = parameter.type.shape
        val items = (shape as? TypeShape.Items)?.let { found -> read("items", parameter, found.item) }
        val whole = queryString(parameter).takeIf { parameter.placement == ParameterPlace.QueryString }
        val fields = objectOf(parameter).takeIf { isObject(shape) }
        return whole ?: fields ?: items ?: read("value", parameter, parameter.type)
    }

    private fun queryString(parameter: ParameterPlan): CodeBlock = CodeBlock.of(
        "%N.queryString(%L, %L, %L)",
        INPUT,
        ServerTypes.mediaType(FORM_TYPE),
        serializer(parameter.type),
        !parameter.type.isNullable,
    )

    private fun objectOf(parameter: ParameterPlan): CodeBlock {
        val named = parameter.type.shape as? TypeShape.Named
        val names = named?.let { found -> fields[found.name] }.orEmpty()
        val set = names.map { name -> CodeBlock.of("%S", name) }.joinToCode(", ")
        return CodeBlock.of(
            "%N.fields(%T.%L, %L, %L, %L, setOf(%L))",
            INPUT,
            ServerTypes.place,
            parameter.placement.name,
            format(parameter),
            serializer(parameter.type),
            !parameter.type.isNullable,
            set,
        )
    }

    private fun read(function: String, parameter: ParameterPlan, single: TypeUse): CodeBlock = CodeBlock.of(
        "%N.%L(%T.%L, %L, %T(%L, %L), %L)",
        INPUT,
        function,
        ServerTypes.place,
        parameter.placement.name,
        format(parameter),
        ServerTypes.text,
        serializer(single),
        parameter.isQuoted,
        !parameter.type.isNullable,
    )

    private fun format(parameter: ParameterPlan): CodeBlock {
        val reserved = CodeBlock.of(", isReservedAllowed = true").takeIf { parameter.isReservedAllowed }
        return CodeBlock.of(
            "%T(%S, %T.%L, %L%L)",
            ServerTypes.format,
            parameter.wireName,
            ServerTypes.style,
            parameter.style,
            parameter.isExploded,
            reserved ?: "",
        )
    }

    data class Reading(
        val code: CodeBlock,
        val checks: List<CodeBlock>,
        val arguments: List<Pair<String, CodeBlock>>,
    )

    companion object {
        fun isObject(shape: TypeShape): Boolean =
            shape is TypeShape.Entries || (shape as? TypeShape.Named)?.kind == ModelKind.Object
    }
}
