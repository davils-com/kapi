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

package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.model.ModelPlanner
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.operation.plan.BodyKind
import com.davils.kapi.codegen.core.operation.plan.FieldKind
import com.davils.kapi.codegen.core.operation.plan.FieldPlan
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.value.ExtensionReading
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition

private const val FILE_PART: String = "com.davils.kapi.runtime.core.wire.FilePart"

private val file: TypeShape = TypeShape.External(FILE_PART, serializer = null)

private val files: Map<FieldKind, TypeShape> = mapOf(
    FieldKind.File to file,
    FieldKind.Files to TypeShape.Items(TypeUse(file)),
)

internal class FieldPlanner(private val scope: PlanScope, private val models: ModelPlanner) {
    private val types: ParameterTypes = ParameterTypes(scope)

    private val names: ClientNames = ClientNames(scope.findings)

    private val kinds: List<Pair<(TypeShape) -> Boolean, FieldKind>> = listOf(
        ::isFile to FieldKind.File,
        ::isFiles to FieldKind.Files,
        types::isText to FieldKind.Text,
        types::isTexts to FieldKind.Texts,
    )

    fun plan(context: String, media: MediaType, body: BodyKind): List<FieldPlan> {
        val definition = definitionOf(media.schema)
        val declared = definition?.properties.orEmpty()
        val properties = declared.filterValues { schema -> !isLeftOut(schema) }
        if (properties.isEmpty()) refuse("a form that is no object with properties", media.location)
        val required = definition?.required.orEmpty()
        return properties.map { (name, schema) ->
            val site = FieldSite(
                wireName = name,
                schema = schema,
                isRequired = name in required,
                encoding = media.encoding[name],
                body = body,
            )
            field(context, site)
        }
    }

    private fun definitionOf(schema: Schema?): SchemaDefinition? {
        val definition = schema as? SchemaDefinition
        val referenced = listOfNotNull(definition?.reference).mapNotNull(scope::componentOf)
        val components = referenced.filterIsInstance<SchemaDefinition>()
        return components.firstOrNull() ?: definition
    }

    private fun isLeftOut(schema: Schema): Boolean {
        val reading = schema.extensions.flag(Keywords.IGNORE) as? ExtensionReading.Present
        val isReadOnly = (schema as? SchemaDefinition)?.isReadOnly == true
        return isReadOnly || reading?.value == true
    }

    private fun field(context: String, site: FieldSite): FieldPlan {
        val name = nameOf(site)
        val use = models.typeOf(site.schema, context + Identifiers.type(name))
        val matching = kinds.filter { (matches, _) -> matches(use.shape) }
        val kind = matching.map { (_, found) -> found }.firstOrNull() ?: FieldKind.Json
        names.audit(name, "The field ${shown(site.wireName)}", site.schema.location)
        audit(site, kind)
        val declared = files[kind]?.let { file -> TypeUse(file) } ?: use
        return FieldPlan(
            name = name,
            wireName = site.wireName,
            type = declared.copy(isNullable = use.isNullable || !site.isRequired),
            kind = kind,
            contentType = site.encoding?.contentType,
        )
    }

    private fun nameOf(site: FieldSite): String {
        val declared = site.schema.extensions.text(Keywords.NAME) as? ExtensionReading.Present
            ?: return Identifiers.member(site.wireName)
        return declared.value
    }

    private fun audit(site: FieldSite, kind: FieldKind) {
        val isFile = kind in files.keys
        val isFormFile = isFile && site.body == BodyKind.Form
        if (isFormFile) refuse("the file ${shown(site.wireName)} in a form", site.schema.location)
        val encoding = site.encoding ?: return
        val isPlain = encoding.style == null && encoding.isExploded == null && encoding.headers.isEmpty()
        if (isPlain && !encoding.isReservedAllowed) return
        refuse("the encoding of the field ${shown(site.wireName)} beyond its content type", encoding.location)
    }

    private fun isFile(shape: TypeShape): Boolean = (shape as? TypeShape.Scalar)?.kind == ScalarKind.Bytes

    private fun isFiles(shape: TypeShape): Boolean = shape is TypeShape.Items && isFile(shape.item.shape)

    private fun refuse(what: String, location: Location?) {
        scope.findings.error(
            CodegenCodes.OPERATION_UNSUPPORTED,
            "The generator has no Kotlin for $what yet",
            location,
        )
    }
}
