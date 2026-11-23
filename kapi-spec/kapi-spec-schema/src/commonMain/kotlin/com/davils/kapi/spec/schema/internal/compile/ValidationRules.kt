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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.schema.internal.compile

import com.davils.kapi.spec.schema.DiagnosticCodes
import com.davils.kapi.spec.schema.internal.dialect.Draft
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import com.davils.kapi.spec.schema.internal.keyword.BoundKeyword
import com.davils.kapi.spec.schema.internal.keyword.ConstKeyword
import com.davils.kapi.spec.schema.internal.keyword.CountKeyword
import com.davils.kapi.spec.schema.internal.keyword.DependenciesKeyword
import com.davils.kapi.spec.schema.internal.keyword.Dependency
import com.davils.kapi.spec.schema.internal.keyword.EnumKeyword
import com.davils.kapi.spec.schema.internal.keyword.FormatKeyword
import com.davils.kapi.spec.schema.internal.keyword.Measure
import com.davils.kapi.spec.schema.internal.keyword.MultipleOfKeyword
import com.davils.kapi.spec.schema.internal.keyword.PatternKeyword
import com.davils.kapi.spec.schema.internal.keyword.RequiredKeyword
import com.davils.kapi.spec.schema.internal.keyword.TypeKeyword
import com.davils.kapi.spec.schema.internal.keyword.Types
import com.davils.kapi.spec.schema.internal.keyword.UniqueItemsKeyword
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object ValidationRules {
    fun type(context: KeywordContext, element: JsonElement): Keyword? {
        val isOpenApi = context.draft == Draft.OpenApi30
        val single = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
        val many = (element as? JsonArray)?.takeIf { !isOpenApi }
        val names = single?.let(::listOf) ?: many?.let { context.texts("type", it) }
        val isValid = names != null && names.isNotEmpty() && names.all { name -> Types.bitOf(name) != null }
        if (!isValid) return context.report("type", "The keyword type must name the types of JSON")
        val isNullable = isOpenApi && (context.members["nullable"] as? JsonPrimitive)?.content == "true"
        val all = names.plus("null").takeIf { isNullable } ?: names
        val mask = all.fold(0) { total, name -> total or (Types.bitOf(name) ?: 0) }
        return TypeKeyword(mask, all, isIntegerStrict = context.draft == Draft.Draft04)
    }

    fun enumeration(context: KeywordContext, element: JsonElement): Keyword? {
        val values = element as? JsonArray ?: return context.report("enum", "The keyword enum must be an array")
        val maximum = context.session.limits.maximumEnumValues
        if (values.size > maximum) {
            return context.report("enum", "The enumeration has more than $maximum values", DiagnosticCodes.LIMIT_EXCEEDED)
        }
        checkNodes(context, "enum", element)
        val canonical = values.mapNotNull { value -> context.literal("enum", value) }
        return EnumKeyword(canonical.distinct().sorted())
    }

    fun constant(context: KeywordContext, element: JsonElement): Keyword? {
        checkNodes(context, "const", element)
        return context.literal("const", element)?.let(::ConstKeyword)
    }

    fun multipleOf(context: KeywordContext, element: JsonElement): Keyword? {
        val divisor = context.number("multipleOf", element) ?: return null
        val isPositive = divisor.sign > 0
        return MultipleOfKeyword(divisor).takeIf { isPositive }
            ?: context.report("multipleOf", "The keyword multipleOf must be greater than zero")
    }

    fun maximum(context: KeywordContext, element: JsonElement): Keyword? = bound(context, element, "maximum", true)

    fun minimum(context: KeywordContext, element: JsonElement): Keyword? = bound(context, element, "minimum", false)

    private fun bound(context: KeywordContext, element: JsonElement, name: String, isMaximum: Boolean): Keyword? {
        val limit = context.number(name, element) ?: return null
        val sibling = "exclusiveMaximum".takeIf { isMaximum } ?: "exclusiveMinimum"
        val isLegacy = context.draft == Draft.Draft04 || context.draft == Draft.OpenApi30
        val isExclusive = isLegacy && (context.members[sibling] as? JsonPrimitive)?.content == "true"
        return BoundKeyword(name, limit, isMaximum, isExclusive)
    }

    fun exclusiveMaximum(context: KeywordContext, element: JsonElement): Keyword? =
        context.number("exclusiveMaximum", element)?.let { BoundKeyword("exclusiveMaximum", it, true, true) }

    fun exclusiveMinimum(context: KeywordContext, element: JsonElement): Keyword? =
        context.number("exclusiveMinimum", element)?.let { BoundKeyword("exclusiveMinimum", it, false, true) }

    fun flag(context: KeywordContext, element: JsonElement, name: String): Keyword? {
        context.flag(name, element)
        return null
    }

    fun count(context: KeywordContext, element: JsonElement, name: String, measure: Measure, isMaximum: Boolean): Keyword? =
        context.count(name, element)?.let { CountKeyword(name, measure, it, isMaximum) }

    fun countOnly(context: KeywordContext, element: JsonElement, name: String): Keyword? {
        context.count(name, element)
        return null
    }

    fun pattern(context: KeywordContext, element: JsonElement): Keyword? {
        val source = context.text("pattern", element) ?: return null
        return context.pattern("pattern", source)?.let { PatternKeyword(source, it) }
    }

    fun uniqueItems(context: KeywordContext, element: JsonElement): Keyword? {
        val isUnique = context.flag("uniqueItems", element) ?: return null
        return UniqueItemsKeyword().takeIf { isUnique }
    }

    fun required(context: KeywordContext, element: JsonElement): Keyword? {
        val names = context.texts("required", element) ?: return null
        return RequiredKeyword(names).takeIf { names.isNotEmpty() }
    }

    fun dependentRequired(context: KeywordContext, element: JsonElement): Keyword? {
        val members = element as? JsonObject
            ?: return context.report("dependentRequired", "The keyword dependentRequired must be an object")
        val entries = members.mapNotNull { (name, value) ->
            context.texts("dependentRequired", value)?.let { Dependency(name, it, null) }
        }
        return DependenciesKeyword("dependentRequired", entries)
    }

    fun format(context: KeywordContext, element: JsonElement): Keyword? {
        val format = context.text("format", element) ?: return null
        return FormatKeyword(format).takeIf { context.profile.formatAssertion }
    }

    private fun checkNodes(context: KeywordContext, keyword: String, element: JsonElement) {
        val maximum = context.session.limits.maximumLiteralNodes
        val nodes = Literals.count(element, maximum.toLong())
        if (nodes > maximum) {
            val message = "The keyword ${SafeText.quoted(keyword)} holds more than $maximum values"
            context.report(keyword, message, DiagnosticCodes.LIMIT_EXCEEDED)
        }
    }
}
