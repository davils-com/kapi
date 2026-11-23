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

package com.davils.kapi.spec.schema.internal.compile

import com.davils.kapi.spec.schema.internal.Uris
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import com.davils.kapi.spec.schema.internal.keyword.DynamicRefKeyword
import com.davils.kapi.spec.schema.internal.keyword.RecursiveRefKeyword
import com.davils.kapi.spec.schema.internal.keyword.RefKeyword
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object CoreRules {
    fun ref(context: KeywordContext, element: JsonElement): Keyword? {
        val reference = context.reference("\$ref", element) ?: return null
        return RefKeyword("\$ref", reference.node)
    }

    fun dynamicRef(context: KeywordContext, element: JsonElement): Keyword? {
        val reference = context.reference("\$dynamicRef", element) ?: return null
        val fragment = Uris.fragmentOf(reference.absolute)
        val declared = membersOf(context, reference.target)?.get("\$dynamicAnchor")
        val isDynamic = fragment.isNotEmpty() && (declared as? JsonPrimitive)?.content == fragment
        val dynamic = DynamicRefKeyword(reference.node, fragment).takeIf { isDynamic }
        return dynamic ?: RefKeyword("\$dynamicRef", reference.node)
    }

    fun recursiveRef(context: KeywordContext, element: JsonElement): Keyword? {
        val reference = context.reference("\$recursiveRef", element) ?: return null
        val declared = membersOf(context, reference.target)?.get("\$recursiveAnchor")
        val isRecursive = (declared as? JsonPrimitive)?.content == "true"
        val recursive = RecursiveRefKeyword(reference.node).takeIf { isRecursive }
        return recursive ?: RefKeyword("\$recursiveRef", reference.node)
    }

    private fun membersOf(context: KeywordContext, target: Target): JsonObject? =
        context.session.elementAt(target) as? JsonObject
}
