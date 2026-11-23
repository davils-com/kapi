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

import com.davils.kapi.spec.schema.internal.dialect.Draft
import com.davils.kapi.spec.schema.internal.dialect.Vocabulary
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import com.davils.kapi.spec.schema.internal.keyword.AdditionalPropertiesKeyword
import com.davils.kapi.spec.schema.internal.keyword.AllOfKeyword
import com.davils.kapi.spec.schema.internal.keyword.AnyOfKeyword
import com.davils.kapi.spec.schema.internal.keyword.ConditionalKeyword
import com.davils.kapi.spec.schema.internal.keyword.ContainsKeyword
import com.davils.kapi.spec.schema.internal.keyword.DependenciesKeyword
import com.davils.kapi.spec.schema.internal.keyword.Dependency
import com.davils.kapi.spec.schema.internal.keyword.NotKeyword
import com.davils.kapi.spec.schema.internal.keyword.OneOfKeyword
import com.davils.kapi.spec.schema.internal.keyword.PatternEntry
import com.davils.kapi.spec.schema.internal.keyword.PatternPropertiesKeyword
import com.davils.kapi.spec.schema.internal.keyword.PropertiesKeyword
import com.davils.kapi.spec.schema.internal.keyword.PropertyNamesKeyword
import com.davils.kapi.spec.schema.internal.keyword.RestItemsKeyword
import com.davils.kapi.spec.schema.internal.keyword.TupleKeyword
import com.davils.kapi.spec.schema.internal.keyword.UnevaluatedItemsKeyword
import com.davils.kapi.spec.schema.internal.keyword.UnevaluatedPropertiesKeyword
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object ApplicatorRules {
    fun allOf(context: KeywordContext, element: JsonElement): Keyword? =
        context.schemas("allOf", element)?.let(::AllOfKeyword)

    fun anyOf(context: KeywordContext, element: JsonElement): Keyword? =
        context.schemas("anyOf", element)?.let(::AnyOfKeyword)

    fun oneOf(context: KeywordContext, element: JsonElement): Keyword? =
        context.schemas("oneOf", element)?.let(::OneOfKeyword)

    fun not(context: KeywordContext, element: JsonElement): Keyword = NotKeyword(context.child("not"))

    fun conditional(context: KeywordContext, element: JsonElement): Keyword {
        val condition = context.child("if")
        val consequence = context.members["then"]?.let { context.child("then") }
        val alternative = context.members["else"]?.let { context.child("else") }
        return ConditionalKeyword(condition, consequence, alternative)
    }

    fun properties(context: KeywordContext, element: JsonElement): Keyword? =
        context.schemaMap("properties", element)?.let(::PropertiesKeyword)

    fun patternProperties(context: KeywordContext, element: JsonElement): Keyword? {
        val members = element as? JsonObject
            ?: return context.report("patternProperties", "The keyword patternProperties must be an object of schemas")
        val entries = members.keys.mapNotNull { source ->
            val matcher = context.pattern("patternProperties", source)
            matcher?.let { PatternEntry(source, it, context.child("patternProperties", source)) }
        }
        return PatternPropertiesKeyword(entries)
    }

    fun additionalProperties(context: KeywordContext, element: JsonElement): Keyword {
        val named = context.active("properties") as? JsonObject
        val patterned = context.active("patternProperties") as? JsonObject
        val names = named?.keys.orEmpty().sorted()
        val patterns = patterned?.keys.orEmpty().mapNotNull { source ->
            com.davils.kapi.spec.schema.internal.PatternBridge.compile(source)
        }
        return AdditionalPropertiesKeyword(context.schemaOrFlag(element, "additionalProperties"), names, patterns)
    }

    fun propertyNames(context: KeywordContext, element: JsonElement): Keyword =
        PropertyNamesKeyword(context.child("propertyNames"))

    fun dependentSchemas(context: KeywordContext, element: JsonElement): Keyword? {
        val entries = context.schemaMap("dependentSchemas", element) ?: return null
        return DependenciesKeyword("dependentSchemas", entries.map { (name, node) -> Dependency(name, null, node) })
    }

    fun dependencies(context: KeywordContext, element: JsonElement): Keyword? {
        val members = element as? JsonObject
            ?: return context.report("dependencies", "The keyword dependencies must be an object")
        val entries = members.mapNotNull { (name, value) ->
            val required = (value as? JsonArray)?.let { context.texts("dependencies", it) }
            val schema = value.takeIf { it !is JsonArray }?.let { context.child("dependencies", name) }
            Dependency(name, required, schema).takeIf { required != null || schema != null }
        }
        return DependenciesKeyword("dependencies", entries)
    }

    fun items(context: KeywordContext, element: JsonElement): Keyword? {
        if (element is JsonArray) return context.report("items", "The keyword items must be a single schema")
        val prefix = context.active("prefixItems") as? JsonArray
        return RestItemsKeyword("items", prefix?.size ?: 0, context.child("items"))
    }

    fun legacyItems(context: KeywordContext, element: JsonElement): Keyword? {
        val items = element as? JsonArray ?: return RestItemsKeyword("items", 0, context.child("items"))
        return TupleKeyword("items", items.indices.map { index -> context.child("items", index.toString()) })
    }

    fun prefixItems(context: KeywordContext, element: JsonElement): Keyword? =
        context.schemas("prefixItems", element)?.let { TupleKeyword("prefixItems", it) }

    fun additionalItems(context: KeywordContext, element: JsonElement): Keyword? {
        val items = context.members["items"] as? JsonArray ?: return null
        return RestItemsKeyword("additionalItems", items.size, context.schemaOrFlag(element, "additionalItems"))
    }

    fun contains(context: KeywordContext, element: JsonElement): Keyword {
        val counts = context.draft != Draft.Draft06 && context.draft != Draft.Draft07 &&
            context.profile.enables(Vocabulary.Validation)
        val minimum = context.members["minContains"]?.takeIf { counts }?.let { context.count("minContains", it) }
        val maximum = context.members["maxContains"]?.takeIf { counts }?.let { context.count("maxContains", it) }
        return ContainsKeyword(
            child = context.child("contains"),
            minimum = minimum ?: 1L,
            maximum = maximum,
            minimumName = "minContains".takeIf { minimum != null } ?: "contains",
            marks = context.draft == Draft.Draft202012,
        )
    }

    fun unevaluatedItems(context: KeywordContext, element: JsonElement): Keyword =
        UnevaluatedItemsKeyword(context.child("unevaluatedItems"))

    fun unevaluatedProperties(context: KeywordContext, element: JsonElement): Keyword =
        UnevaluatedPropertiesKeyword(context.child("unevaluatedProperties"))
}
