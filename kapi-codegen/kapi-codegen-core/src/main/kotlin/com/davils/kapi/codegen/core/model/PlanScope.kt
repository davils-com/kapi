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

package com.davils.kapi.codegen.core.model

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.ModelCatalog
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.naming.NameClaim
import com.davils.kapi.codegen.core.naming.NameTable
import com.davils.kapi.codegen.core.pipeline.Findings
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.resolve

private const val SEPARATOR: Char = '/'

private const val COMPONENT_DEPTH: Int = 3

private val componentPath: List<String> = listOf("components", "schemas")

internal class PlanScope(
    val document: Document,
    val names: Map<String, String>,
    val findings: Findings,
    private val table: NameTable,
    private val catalogs: List<ModelCatalog>,
) {
    private val declared: MutableList<ModelPlan> = mutableListOf()

    private val schemas: Map<String, Schema> = document.components?.schemas.orEmpty()

    private val origins: MutableMap<String, Location> = mutableMapOf()

    private val inline: MutableMap<Location, String> = mutableMapOf()

    val plans: List<ModelPlan> get() = declared.map(::linked).sortedBy(ModelPlan::name)

    val documentUri: String get() = document.location?.run { document.value }.orEmpty()

    val documentName: String get() = documentUri.substringAfterLast(SEPARATOR)

    fun component(key: String): Schema? = schemas[key]

    fun contentOf(content: Map<String, Referable<MediaType>>): Map<String, MediaType> =
        content.mapNotNull { (type, media) -> document.resolve(media)?.let { resolved -> type to resolved } }.toMap()

    fun foreign(reference: Reference): TypeShape.Named? = ModelCatalogs.foreign(catalogs, reference)

    fun extending(base: Schema): List<String> {
        val children = schemas.filterValues { schema -> extends(schema, base) }
        return children.keys.sorted()
    }

    fun componentOf(reference: Reference): Schema? = located(reference)?.second

    fun keyOf(reference: Reference): String? = located(reference)?.first

    fun located(reference: Reference): Pair<String, Schema>? {
        val tokens = reference.target.pointer.tokens
        val isComponent = tokens.size == COMPONENT_DEPTH && tokens.take(2) == componentPath
        val isLocal = reference.target.document == document.location?.document
        val last = tokens.lastOrNull().orEmpty()
        val key = last.takeIf { isComponent && isLocal }.orEmpty()
        val schema = schemas[key] ?: return null
        return key to schema
    }

    fun plannedName(schema: Schema): String? = schema.location?.let(inline::get)

    fun remember(schema: Schema, name: String) {
        schema.location?.let { location -> inline[location] = name }
    }

    fun declare(plan: ModelPlan) {
        declared += plan
    }

    fun claim(name: String, schema: Schema) {
        table.claim(NameClaim(name = name, origin = "The schema ${originOf(schema)}", location = schema.location))
    }

    fun claimName(name: String, origin: String) {
        table.claim(NameClaim(name, origin, location = null))
    }

    fun originOf(schema: Schema): String {
        val location = schema.location ?: return ""
        val origin = location.document.value.substringAfterLast(SEPARATOR) + "#" + location.pointer
        origins[origin] = location
        return origin
    }

    fun locationOf(origin: String): Location? = origins[origin]

    private fun extends(schema: Schema, base: Schema): Boolean {
        val definition = schema as? SchemaDefinition
        val members = definition?.allOf.orEmpty().filterIsInstance<SchemaDefinition>()
        return members.any { member -> member.reference?.let(::componentOf) === base }
    }

    private fun linked(plan: ModelPlan): ModelPlan {
        val objectPlan = plan as? ModelPlan.ObjectPlan ?: return plan
        val unions = declared.filterIsInstance<ModelPlan.UnionPlan>()
        val owners = unions.filter { union -> union.members.any { member -> member.type == plan.name } }
        return objectPlan.copy(unions = owners.map(ModelPlan.UnionPlan::name).sorted())
    }

    fun unchecked(schema: Schema, keyword: String) {
        findings.warn(
            CodegenCodes.SCHEMA_UNCHECKED,
            "The generated validate() does not check $keyword",
            schema.location,
        )
    }

    fun unsupported(schema: Schema, what: String) {
        findings.error(
            CodegenCodes.SCHEMA_UNSUPPORTED,
            "The generator has no Kotlin for $what yet",
            schema.location,
        )
    }
}
