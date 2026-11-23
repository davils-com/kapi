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

import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.schema.FixedSchema
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType

private const val TYPE: String = "type"

private const val SERIALIZER: String = "serializer"

private val json: TypeUse = TypeUse(TypeShape.Scalar(ScalarKind.Json))

private val formats: Map<SchemaType, Map<String, ScalarKind>> = mapOf(
    SchemaType.String to mapOf(
        "" to ScalarKind.Text,
        "date-time" to ScalarKind.Instant,
        "byte" to ScalarKind.Bytes,
        "binary" to ScalarKind.Bytes,
    ),
    SchemaType.Integer to mapOf(
        "" to ScalarKind.Long,
        "int32" to ScalarKind.Int,
        "uint64" to ScalarKind.UnsignedLong,
    ),
    SchemaType.Number to mapOf("" to ScalarKind.Double, "float" to ScalarKind.Float),
    SchemaType.Boolean to mapOf("" to ScalarKind.Flag),
)

internal class SchemaTypes(private val scope: PlanScope, private val planner: ModelPlanner) {
    private val references: SchemaReferences = SchemaReferences(scope, this)

    private val constants: SchemaConstants = SchemaConstants(scope)

    fun typeOf(schema: Schema, context: String): TypeUse = when (schema) {
        is FixedSchema -> fixed(schema)
        is SchemaDefinition -> defined(schema, context)
    }

    private fun fixed(schema: FixedSchema): TypeUse {
        if (!schema.isAccepting) scope.unsupported(schema, "a schema that accepts nothing")
        return json
    }

    private fun defined(definition: SchemaDefinition, context: String): TypeUse {
        SchemaSupport.audit(scope, definition)
        val use = external(definition)
            ?: references.referenced(definition)
            ?: optional(definition, context)
            ?: alias(definition, context)
            ?: model(definition, context)
            ?: typed(definition, context)
        val hasNull = definition.enumeration.orEmpty().any { value -> value is Value.Null }
        val isNullable = use.isNullable || hasNull || SchemaType.Null in definition.types
        return use.copy(isNullable = isNullable)
    }

    private fun external(definition: SchemaDefinition): TypeUse? {
        val value = definition.extensions[Keywords.TYPE]
        val entries = (value as? Value.Mapping)?.entries.orEmpty()
        val type = textOf(value) ?: textOf(entries[TYPE])
        return type?.let { name -> TypeUse(TypeShape.External(type = name, serializer = textOf(entries[SERIALIZER]))) }
    }

    private fun optional(definition: SchemaDefinition, context: String): TypeUse? {
        val alternative = SchemaKinds.optionalOf(definition)
        return alternative?.let { schema -> typeOf(schema, context).copy(isNullable = true) }
    }

    private fun alias(definition: SchemaDefinition, context: String): TypeUse? {
        val member = SchemaKinds.aliasOf(definition) ?: return null
        return typeOf(member, context)
    }

    private fun model(definition: SchemaDefinition, context: String): TypeUse? {
        val kind = SchemaKinds.kindOf(definition) ?: return null
        val name = scope.plannedName(definition) ?: planned(definition, context)
        return TypeUse(TypeShape.Named(name, kind))
    }

    private fun planned(definition: SchemaDefinition, context: String): String {
        scope.remember(definition, context)
        scope.claim(context, definition)
        planner.declare(context, definition)
        return context
    }

    private fun typed(definition: SchemaDefinition, context: String): TypeUse {
        val type = SchemaSupport.typesOf(definition).firstOrNull()
        val constant = constants.kindOf(definition).takeIf { type == null }
        val scalars = formats[type] ?: constant?.let { kind -> mapOf("" to kind) }
        if (scalars != null) return scalar(definition, scalars)
        constants.refuseStructured(definition)
        if (definition.dependentRequired.isNotEmpty()) scope.unchecked(definition, "dependentRequired")
        return structured(definition, context, type)
    }

    private fun scalar(definition: SchemaDefinition, scalars: Map<String, ScalarKind>): TypeUse {
        val kind = scalars[definition.format.orEmpty()] ?: scalars.getValue("")
        val rules = SchemaRules.scalar(definition, kind) + constants.rules(definition, kind)
        val hints = definition.examples + listOfNotNull(definition.default)
        return TypeUse(TypeShape.Scalar(kind), rules = rules, hints = hints)
    }

    private fun structured(definition: SchemaDefinition, context: String, type: SchemaType?): TypeUse {
        if (type == SchemaType.Array) {
            val items = TypeShape.Items(nested(definition.items, context + "Item"))
            return TypeUse(items, rules = SchemaRules.items(definition))
        }
        val isMap = type == SchemaType.Object || definition.additionalProperties != null
        val entries = TypeShape.Entries(nested(definition.additionalProperties, context + "Value"))
        return TypeUse(entries, rules = SchemaRules.entries(definition)).takeIf { isMap } ?: json
    }

    private fun nested(schema: Schema?, context: String): TypeUse {
        if (schema == null) return json
        return typeOf(schema, context)
    }

    private fun textOf(value: Value?): String? = (value as? Value.Text)?.value
}
