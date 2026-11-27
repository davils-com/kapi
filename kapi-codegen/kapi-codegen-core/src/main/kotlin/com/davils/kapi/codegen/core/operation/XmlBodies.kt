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
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.ModelPlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.pipeline.place
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.Xml

private const val COMPONENT: String = "#/components/schemas/"

private const val RESERVED: String = "xml"

private const val PUNCTUATION: String = "_-."

private const val ROOTLESS: String = "a body that is no object of components/schemas, whose name names the root element"

internal class XmlBodies(private val scope: PlanScope) {
    fun audit(schema: Schema, media: MediaType, type: TypeUse) {
        keywords(schema, mutableSetOf())
        val plans = scope.plans.associateBy(ModelPlan::name)
        val root = rootOf(type, plans) ?: return refuse(ROOTLESS, media.place)
        objectOf(root, plans, mutableSetOf())
    }

    private fun rootOf(type: TypeUse, plans: Map<String, ModelPlan>): ModelPlan.ObjectPlan? {
        val named = type.shape as? TypeShape.Named
        val local = named?.takeIf { shape -> shape.packageName == null }
        val plan = local?.let { shape -> plans[shape.name] } as? ModelPlan.ObjectPlan
        return plan?.takeIf { found -> nameOf(found.origin) != null }
    }

    private fun objectOf(plan: ModelPlan.ObjectPlan, plans: Map<String, ModelPlan>, seen: MutableSet<String>) {
        if (!seen.add(plan.name)) return
        val location = scope.locationOf(plan.origin)
        plan.properties.forEach { property ->
            if (!isName(property.jsonName)) {
                refuse("the property ${shown(property.jsonName)}, which is no XML name", location)
            }
            typeOf(use = property.type, plans = plans, seen = seen, location = location)
        }
    }

    private fun typeOf(use: TypeUse, plans: Map<String, ModelPlan>, seen: MutableSet<String>, location: Location?) {
        when (val shape = use.shape) {
            is TypeShape.Scalar -> scalar(shape, location)
            is TypeShape.Named -> named(shape = shape, plans = plans, seen = seen, location = location)
            is TypeShape.Items -> items(shape = shape, plans = plans, seen = seen, location = location)
            is TypeShape.Entries -> refuse("a map, whose keys XML has no element for", location)
            is TypeShape.External -> refuse("the type ${shown(shape.type)} of the application", location)
        }
    }

    private fun scalar(shape: TypeShape.Scalar, location: Location?) {
        if (shape.kind == ScalarKind.Json) refuse("a free-form value", location)
    }

    private fun named(
        shape: TypeShape.Named,
        plans: Map<String, ModelPlan>,
        seen: MutableSet<String>,
        location: Location?,
    ) {
        val plan = plans[shape.name].takeIf { shape.packageName == null }
        when (plan) {
            is ModelPlan.ObjectPlan -> objectOf(plan, plans, seen)
            is ModelPlan.EnumPlan -> Unit
            is ModelPlan.UnionPlan -> refuse("the union ${plan.name}, whose members XML cannot tell apart", location)
            null -> refuse("the model ${shape.name} of another document", location)
        }
    }

    private fun items(
        shape: TypeShape.Items,
        plans: Map<String, ModelPlan>,
        seen: MutableSet<String>,
        location: Location?,
    ) {
        val item = shape.item
        val isNested = item.shape is TypeShape.Items || item.shape is TypeShape.Entries
        if (isNested || item.isNullable) {
            refuse("an array of arrays, maps or nulls, which unwrapped elements cannot carry", location)
            return
        }
        typeOf(use = item, plans = plans, seen = seen, location = location)
    }

    private fun keywords(schema: Schema, seen: MutableSet<Location>) {
        val definition = schema as? SchemaDefinition ?: return
        if (!seen.add(definition.place)) return
        definition.xml?.takeIf(::isMapped)?.let { xml ->
            refuse("the xml object, whose attributes, wrapping, names and namespaces are not mapped yet", xml.place)
        }
        val referenced = definition.reference?.let(scope::componentOf)
        val members = definition.allOf + definition.anyOf + definition.oneOf + definition.prefixItems
        val nested = listOfNotNull(referenced, definition.items, definition.additionalProperties)
        (definition.properties.values + members + nested).forEach { child -> keywords(child, seen) }
    }

    private fun isMapped(xml: Xml): Boolean {
        val texts = listOfNotNull(xml.name, xml.namespace, xml.prefix, xml.nodeType)
        return texts.isNotEmpty() || xml.isAttribute || xml.isWrapped
    }

    private fun refuse(what: String, location: Location?) {
        scope.findings.error(
            CodegenCodes.XML_UNSUPPORTED,
            "The generator has no faithful XML for $what",
            location,
        )
    }

    companion object {
        fun nameOf(origin: String): String? {
            val key = origin.substringAfter(COMPONENT, missingDelimiterValue = "")
            return key.takeIf { found -> isName(found) && '.' !in found }
        }

        fun isName(text: String): Boolean {
            val first = text.firstOrNull() ?: return false
            val isStart = first.isAsciiLetter() || first == '_'
            val isRest = text.all { char -> char.isAsciiLetter() || char in '0'..'9' || char in PUNCTUATION }
            return isStart && isRest && !text.startsWith(RESERVED, ignoreCase = true)
        }

        private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
    }
}
