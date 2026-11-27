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

import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.BodyPlan
import com.davils.kapi.codegen.core.operation.plan.FieldKind
import com.davils.kapi.codegen.core.operation.plan.FieldPlan
import com.squareup.kotlinpoet.CodeBlock

private const val JSON: String = "application/json"

private const val GUARD: String = "if (%N != null) "

private const val BODY: String = "body"

private val forms: Map<FieldKind, String> = mapOf(FieldKind.Text to "field", FieldKind.Texts to "fields")

internal class BodyCode(private val serializers: SerializerCode) {
    fun of(body: BodyPlan): CodeBlock = when (body.kind) {
        BodyKind.Json -> whole(body, json(BODY, body.type))
        BodyKind.Xml -> encoded(body)
        BodyKind.Binary -> whole(body, CodeBlock.of(BODY))
        BodyKind.Upload -> upload(body)
        BodyKind.Form -> block(CodeBlock.of("form {\n"), body.fields.map(::formField))
        BodyKind.Multipart -> block(CodeBlock.of("multipart {\n"), body.fields.map(::part))
    }

    private fun whole(body: BodyPlan, content: CodeBlock): CodeBlock {
        val guard = CodeBlock.of(GUARD, BODY).takeIf { body.type.isNullable }
        return CodeBlock.of("%Lbody(%L, %L)\n", guard ?: "", mediaType(body.contentType), content)
    }

    private fun encoded(body: BodyPlan): CodeBlock {
        val guard = CodeBlock.of(GUARD, BODY).takeIf { body.type.isNullable }
        val serializer = serializers.of(body.type.copy(isNullable = false))
        val type = mediaType(body.contentType)
        return CodeBlock.of("%Lencoded(client.encode(%L, %N, %L))\n", guard ?: "", serializer, BODY, type)
    }

    private fun upload(body: BodyPlan): CodeBlock {
        val guard = CodeBlock.of(GUARD, BODY).takeIf { body.type.isNullable }
        return CodeBlock.of("%Lupload(%L, %N)\n", guard ?: "", mediaType(body.contentType), BODY)
    }

    private fun block(head: CodeBlock, fields: List<CodeBlock>): CodeBlock {
        val code = CodeBlock.builder()
        code.add(head)
        code.indent()
        fields.forEach { field -> code.add("%L\n", field) }
        code.unindent()
        code.add("}\n")
        return code.build()
    }

    private fun formField(field: FieldPlan): CodeBlock {
        val function = forms[field.kind] ?: return call("field", field, jsonText(field))
        return call(function, field, value(field))
    }

    private fun part(field: FieldPlan): CodeBlock = when (field.kind) {
        FieldKind.Text -> call("text", field, value(field))
        FieldKind.Texts -> call("texts", field, value(field))
        FieldKind.File -> call("file", field, file(field))
        FieldKind.Files -> call("files", field, file(field))
        FieldKind.Json -> call("part", field, jsonPart(field))
    }

    private fun call(function: String, field: FieldPlan, arguments: CodeBlock): CodeBlock {
        val guard = CodeBlock.of(GUARD, field.name).takeIf { field.type.isNullable }
        return CodeBlock.of("%L%L(%S, %L)", guard ?: "", function, field.wireName, arguments)
    }

    private fun value(field: FieldPlan): CodeBlock {
        val items = field.type.shape as? TypeShape.Items ?: return ValueCode.text(field.type.shape, field.name)
        return ValueCode.texts(items, field.name)
    }

    private fun file(field: FieldPlan): CodeBlock {
        val type = field.contentType ?: return CodeBlock.of("%N", field.name)
        return CodeBlock.of("%N, %L", field.name, mediaType(type))
    }

    private fun jsonText(field: FieldPlan): CodeBlock =
        CodeBlock.of("%L.%M()", json(field.name, field.type), ClientTypes.decodeToString)

    private fun jsonPart(field: FieldPlan): CodeBlock =
        CodeBlock.of("%L, %L", mediaType(field.contentType ?: JSON), json(field.name, field.type))

    private fun mediaType(text: String): CodeBlock = CodeBlock.of("%T.parse(%S)", ClientTypes.mediaType, text)

    private fun json(name: String, type: TypeUse): CodeBlock =
        CodeBlock.of("client.json(%L, %N)", serializers.of(type.copy(isNullable = false)), name)
}
