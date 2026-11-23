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

private const val ENUM: String = "enum"

private const val CONST: String = "const"

private const val EXCLUSIVE_MAXIMUM: String = "exclusiveMaximum"

private const val EXCLUSIVE_MINIMUM: String = "exclusiveMinimum"

private const val DEPENDENT_REQUIRED: String = "dependentRequired"

internal object ValidationRules {
    fun type(context: KeywordContext, element: JsonElement): Keyword? {
        val isOpenApi = context.draft == Draft.OpenApi30
        val single = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
        val many = (element as? JsonArray)?.takeIf { !isOpenApi }
        val names = many?.let { context.texts("type", it) } ?: listOfNotNull(single)
        val isValid = names.isNotEmpty() && names.all { name -> Types.bitOf(name) != null }
        if (!isValid) return context.report(keyword = "type", message = "The keyword type must name the types of JSON")
        val isNullable = isOpenApi && (context.members["nullable"] as? JsonPrimitive)?.content == "true"
        val all = names.plus("null").takeIf { isNullable } ?: names
        val mask = all.fold(0) { total, name -> total or (Types.bitOf(name) ?: 0) }
        return TypeKeyword(mask, all, isIntegerStrict = context.draft == Draft.Draft04)
    }

    fun enumeration(context: KeywordContext, element: JsonElement): Keyword? {
        val values = element as? JsonArray ?: return context.report(
            keyword = ENUM,
            message = "The keyword enum must be an array",
        )
        val maximum = context.session.limits.maximumEnumValues
        val isBounded = values.size <= maximum
        if (!isBounded) {
            context.report(
                keyword = ENUM,
                message = "The enumeration has more than $maximum values",
                code = DiagnosticCodes.LIMIT_EXCEEDED,
            )
        }
        val canonical = values.takeIf { isBounded && checkNodes(context, ENUM, element) }?.mapNotNull { value ->
            context.literal(ENUM, value)
        }
        return canonical?.run { EnumKeyword(distinct().sorted()) }
    }

    fun constant(context: KeywordContext, element: JsonElement): Keyword? {
        val isBounded = checkNodes(context, CONST, element)
        val literal = context.literal(CONST, element).takeIf { isBounded }
        return literal?.let(::ConstKeyword)
    }

    fun multipleOf(context: KeywordContext, element: JsonElement): Keyword? {
        val divisor = context.number("multipleOf", element) ?: return null
        val isPositive = divisor.sign > 0
        return MultipleOfKeyword(divisor).takeIf { isPositive }
            ?: context.report(keyword = "multipleOf", message = "The keyword multipleOf must be greater than zero")
    }

    fun maximum(context: KeywordContext, element: JsonElement): Keyword? =
        bound(context = context, element = element, name = "maximum", isMaximum = true)

    fun minimum(context: KeywordContext, element: JsonElement): Keyword? =
        bound(context = context, element = element, name = "minimum", isMaximum = false)

    private fun bound(context: KeywordContext, element: JsonElement, name: String, isMaximum: Boolean): Keyword? {
        val limit = context.number(name, element) ?: return null
        val sibling = EXCLUSIVE_MAXIMUM.takeIf { isMaximum } ?: EXCLUSIVE_MINIMUM
        val isLegacy = context.draft == Draft.Draft04 || context.draft == Draft.OpenApi30
        val isExclusive = isLegacy && (context.members[sibling] as? JsonPrimitive)?.content == "true"
        return BoundKeyword(name = name, limit = limit, isMaximum = isMaximum, isExclusive = isExclusive)
    }

    fun exclusiveMaximum(context: KeywordContext, element: JsonElement): Keyword? =
        context.number(EXCLUSIVE_MAXIMUM, element)?.let {
            BoundKeyword(name = EXCLUSIVE_MAXIMUM, limit = it, isMaximum = true, isExclusive = true)
        }

    fun exclusiveMinimum(context: KeywordContext, element: JsonElement): Keyword? =
        context.number(EXCLUSIVE_MINIMUM, element)?.let {
            BoundKeyword(name = EXCLUSIVE_MINIMUM, limit = it, isMaximum = false, isExclusive = true)
        }

    fun flag(context: KeywordContext, element: JsonElement, name: String): Keyword? {
        context.flag(name, element)
        return null
    }

    fun count(context: KeywordContext, element: JsonElement, counted: Counted): Keyword? {
        val bound = context.count(counted.name, element) ?: return null
        return CountKeyword(
            name = counted.name,
            measure = counted.measure,
            bound = bound,
            isMaximum = counted.isMaximum,
        )
    }

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
            ?: return context.report(
                keyword = DEPENDENT_REQUIRED,
                message = "The keyword dependentRequired must be an object",
            )
        val entries = members.mapNotNull { (name, value) ->
            context.texts(DEPENDENT_REQUIRED, value)?.let { Dependency(name, it, schema = null) }
        }
        return DependenciesKeyword(DEPENDENT_REQUIRED, entries)
    }

    fun format(context: KeywordContext, element: JsonElement): Keyword? {
        val format = context.text("format", element) ?: return null
        return FormatKeyword(format).takeIf { context.profile.isFormatAsserted }
    }

    private fun checkNodes(context: KeywordContext, keyword: String, element: JsonElement): Boolean {
        val maximum = context.session.limits.maximumLiteralNodes
        val nodes = Literals.count(element, maximum.toLong())
        if (nodes > maximum) {
            val message = "The keyword ${SafeText.quoted(keyword)} holds more than $maximum values"
            context.report(keyword, message, DiagnosticCodes.LIMIT_EXCEEDED)
        }
        return nodes <= maximum
    }
}
