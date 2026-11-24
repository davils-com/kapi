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
import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.ModelKind
import com.davils.kapi.codegen.core.model.plan.DependencyPlan
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.PropertyAccess
import com.davils.kapi.codegen.core.model.plan.PropertyPlan
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.naming.NameClaim
import com.davils.kapi.codegen.core.naming.NameTable
import com.davils.kapi.codegen.core.pipeline.isTrue
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.codegen.core.pipeline.valueOrNull
import com.davils.kapi.spec.openapi.model.schema.FixedSchema
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition

internal class ModelPlanner(private val scope: PlanScope) {
    private val types: SchemaTypes = SchemaTypes(scope, this)

    private val enums: EnumPlanner = EnumPlanner(scope)

    private val composition: Composition = Composition(scope)

    private val unions: UnionPlanner = UnionPlanner(scope, composition)

    fun plan(): List<ModelPlan> {
        val schemas = scope.document.components
            ?.schemas
            .orEmpty()
        schemas.toSortedMap().forEach { (key, schema) -> component(scope.names.getValue(key), schema) }
        return scope.plans
    }

    fun typeOf(schema: Schema, context: String): TypeUse = types.typeOf(schema, context)

    fun declare(name: String, definition: SchemaDefinition) {
        val plan = when (SchemaKinds.kindOf(definition)) {
            ModelKind.Enum -> enums.plan(name, definition)
            ModelKind.Union -> unions.plan(name, definition)
            ModelKind.Object, null -> objectPlan(name, definition)
        }
        scope.declare(plan)
    }

    private fun component(name: String, schema: Schema) {
        val model = (schema as? SchemaDefinition)?.takeIf { SchemaKinds.kindOf(schema) != null }
        if (model == null) {
            types.typeOf(schema, name)
            return
        }
        SchemaSupport.audit(scope, model)
        declare(name, model)
    }

    private fun objectPlan(name: String, definition: SchemaDefinition): ModelPlan.ObjectPlan {
        val members = NameTable(scope.findings)
        val shape = composition.shapeOf(definition)
        val kept = shape.properties.filterNot { (jsonName, schema) -> isIgnored(shape, jsonName, schema) }
        val properties = kept.map { (jsonName, schema) -> property(name, shape, jsonName to schema) }
        properties.forEach { property ->
            val origin = "The property ${shown(property.jsonName)}"
            members.claim(NameClaim(name = property.name, origin = origin, location = definition.location))
        }
        val dependencies = shape.dependencies.flatMap { (trigger, names) ->
            dependencies(definition, properties, trigger to names)
        }
        return ModelPlan.ObjectPlan(
            name = name,
            origin = scope.originOf(definition),
            properties = properties,
            dependencies = dependencies,
            description = SchemaKinds.describe(definition),
        )
    }

    private fun dependencies(
        definition: SchemaDefinition,
        properties: List<PropertyPlan>,
        entry: Pair<String, List<String>>,
    ): List<DependencyPlan> {
        val byName = properties.associateBy(PropertyPlan::jsonName)
        val unknown = (entry.second + entry.first).filter { name -> name !in byName }
        unknown.forEach { name ->
            scope.unsupported(definition, "dependentRequired that names ${shown(name)}, which is no property")
        }
        val trigger = byName[entry.first] ?: return emptyList()
        val dependents = entry.second.mapNotNull(byName::get).filter { property -> property.type.isNullable }
        return dependents.map { dependent -> DependencyPlan(trigger, dependent) }
    }

    private fun isIgnored(owner: ObjectShape, jsonName: String, schema: Schema): Boolean {
        val isForbidden = (schema as? FixedSchema)?.isAccepting == false
        val isIgnored = isForbidden || flag(schema, isSensitive = false)
        if (isIgnored && jsonName in owner.required) {
            scope.findings.error(
                CodegenCodes.PROPERTY_IGNORED,
                "The property ${shown(jsonName)} is required and cannot be left out",
                schema.location,
            )
        }
        return isIgnored
    }

    private fun property(owner: String, shape: ObjectShape, entry: Pair<String, Schema>): PropertyPlan {
        val (jsonName, schema) = entry
        val name = schema.extensions.text(Keywords.NAME).valueOrNull() ?: Identifiers.member(jsonName)
        val use = types.typeOf(schema, owner + Identifiers.type(name))
        val isOptional = jsonName !in shape.required
        return PropertyPlan(
            name = name,
            jsonName = jsonName,
            type = use.copy(isNullable = use.isNullable || isOptional),
            isOptional = isOptional,
            isSensitive = flag(schema, isSensitive = true),
            access = accessOf(schema),
            description = (schema as? SchemaDefinition)?.let(SchemaKinds::describe),
        )
    }

    private fun accessOf(schema: Schema): PropertyAccess {
        val definition = schema as? SchemaDefinition
        val write = PropertyAccess.Write.takeIf { definition?.isWriteOnly == true }
        return PropertyAccess.Read.takeIf { definition?.isReadOnly == true } ?: write ?: PropertyAccess.Both
    }

    private fun flag(schema: Schema, isSensitive: Boolean): Boolean {
        val keyword = Keywords.SENSITIVE.takeIf { isSensitive } ?: Keywords.IGNORE
        return schema.extensions.flag(keyword).isTrue()
    }
}
