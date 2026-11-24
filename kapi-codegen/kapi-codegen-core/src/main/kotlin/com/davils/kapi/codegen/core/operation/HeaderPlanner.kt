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

import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.model.ModelPlanner
import com.davils.kapi.codegen.core.model.PlanScope
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.naming.NameClaim
import com.davils.kapi.codegen.core.naming.NameTable
import com.davils.kapi.codegen.core.operation.plan.HeaderPlan
import com.davils.kapi.codegen.core.pipeline.isTrue
import com.davils.kapi.codegen.core.pipeline.shown
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.resolve

private const val CONTENT_TYPE: String = "content-type"

private const val SUFFIX: String = "Header"

private val taken: Set<String> = setOf("body", "content", "headers", "status", "exchange", "decoding", "client")

private val readers: Map<ScalarKind, String> = mapOf(
    ScalarKind.Text to HeaderPlan.TEXT,
    ScalarKind.Int to "int",
    ScalarKind.Long to "long",
    ScalarKind.Double to "double",
    ScalarKind.Float to "float",
    ScalarKind.Flag to "flag",
)

internal class HeaderPlanner(private val scope: PlanScope, private val models: ModelPlanner) {
    fun plan(context: String, response: Response): List<HeaderPlan> {
        val declared = response.headers.entries.sortedBy { (name, _) -> name.lowercase() }
        val plans = declared.flatMap { (name, referable) ->
            val resolved = listOfNotNull(scope.document.resolve(referable))
            resolved.filter { header -> isTyped(name, header) }.mapNotNull { header -> plan(context, name, header) }
        }
        val names = NameTable(scope.findings)
        plans.forEach { plan ->
            val origin = "The header ${shown(plan.wireName)}"
            names.claim(NameClaim(name = plan.name, origin = origin, location = response.location))
        }
        return plans
    }

    private fun isTyped(name: String, header: Header): Boolean {
        val isSensitive = header.extensions.flag(Keywords.SENSITIVE).isTrue()
        return name.lowercase() != CONTENT_TYPE && !isSensitive
    }

    private fun plan(context: String, wireName: String, header: Header): HeaderPlan? {
        val member = Identifiers.member(wireName)
        val name = (member + SUFFIX).takeIf { member in taken } ?: member
        val schemas = listOfNotNull(header.schema)
        val uses = schemas.map { schema -> models.typeOf(schema, context + Identifiers.type(name)) }
        val use = uses.firstOrNull { type -> readerOf(type) != null } ?: return null
        return HeaderPlan(
            name = name,
            wireName = wireName,
            type = TypeUse(use.shape, isNullable = use.isNullable || !header.isRequired),
            reader = readerOf(use).orEmpty(),
        )
    }

    private fun readerOf(use: TypeUse): String? {
        val scalar = use.shape as? TypeShape.Scalar ?: return null
        return readers[scalar.kind]
    }
}
