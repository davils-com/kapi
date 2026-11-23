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
import com.davils.kapi.spec.schema.internal.dialect.Profile
import com.davils.kapi.spec.schema.internal.dialect.Vocabulary
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import com.davils.kapi.spec.schema.internal.keyword.Measure
import kotlinx.serialization.json.JsonElement

private const val EXCLUSIVE_MAXIMUM: String = "exclusiveMaximum"

private const val EXCLUSIVE_MINIMUM: String = "exclusiveMinimum"

internal object KeywordRules {
    private val ALL: Set<Draft> = Draft.entries.toSet()

    private val JSON: Set<Draft> = ALL - Draft.OpenApi30

    private val FROM_06: Set<Draft> = JSON - Draft.Draft04

    private val FROM_07: Set<Draft> = FROM_06 - Draft.Draft06

    private val MODERN: Set<Draft> = setOf(Draft.Draft201909, Draft.Draft202012)

    private val UNTIL_07: Set<Draft> = setOf(Draft.Draft04, Draft.Draft06, Draft.Draft07)

    private val UNTIL_2019: Set<Draft> = UNTIL_07 + Draft.Draft201909

    private val BOOLEAN_EXCLUSIVE: Set<Draft> = setOf(Draft.Draft04, Draft.OpenApi30)

    private val rules: List<KeywordRule> = buildList {
        RuleScope(this, Vocabulary.Core).core()
        RuleScope(this, Vocabulary.Applicator).applicators()
        RuleScope(this, Vocabulary.Unevaluated).unevaluated()
        RuleScope(this, Vocabulary.Validation).validations()
        RuleScope(this, null).unbound()
    }

    private val byName: Map<String, List<KeywordRule>> = rules.groupBy { it.name }

    fun ruleOf(name: String, profile: Profile): KeywordRule? {
        val candidates = byName[name] ?: return null
        return candidates.firstOrNull { rule -> profile.draft in rule.drafts && profile.enables(rule.vocabulary) }
    }

    private fun RuleScope.core() {
        keyword("\$ref", ALL, CoreRules::ref)
        keyword("\$dynamicRef", setOf(Draft.Draft202012), CoreRules::dynamicRef)
        keyword("\$recursiveRef", setOf(Draft.Draft201909), CoreRules::recursiveRef)
        map("\$defs", MODERN)
    }

    private fun RuleScope.applicators() {
        list("allOf", ALL, ApplicatorRules::allOf)
        list("anyOf", ALL, ApplicatorRules::anyOf)
        list("oneOf", ALL, ApplicatorRules::oneOf)
        single("not", ALL, contextual(ApplicatorRules::not))
        single("if", FROM_07, contextual(ApplicatorRules::conditional))
        single("then", FROM_07)
        single("else", FROM_07)
        map("properties", ALL, ApplicatorRules::properties)
        map("patternProperties", JSON, ApplicatorRules::patternProperties)
        single("additionalProperties", ALL, ApplicatorRules::additionalProperties)
        single("propertyNames", FROM_06, contextual(ApplicatorRules::propertyNames))
        map("dependentSchemas", MODERN, ApplicatorRules::dependentSchemas)
        single("items", setOf(Draft.Draft202012, Draft.OpenApi30), ApplicatorRules::items)
        singleOrList("items", UNTIL_2019, ApplicatorRules::legacyItems)
        list("prefixItems", setOf(Draft.Draft202012), ApplicatorRules::prefixItems)
        single("additionalItems", UNTIL_2019, ApplicatorRules::additionalItems)
        single("contains", FROM_06, contextual(ApplicatorRules::contains))
    }

    private fun RuleScope.unevaluated() {
        single("unevaluatedItems", MODERN, contextual(ApplicatorRules::unevaluatedItems))
        single("unevaluatedProperties", MODERN, contextual(ApplicatorRules::unevaluatedProperties))
    }

    private fun RuleScope.validations() {
        keyword("type", ALL, ValidationRules::type)
        keyword("enum", ALL, ValidationRules::enumeration)
        keyword("const", FROM_06, ValidationRules::constant)
        keyword("multipleOf", ALL, ValidationRules::multipleOf)
        keyword("maximum", ALL, ValidationRules::maximum)
        keyword("minimum", ALL, ValidationRules::minimum)
        keyword(EXCLUSIVE_MAXIMUM, FROM_06, ValidationRules::exclusiveMaximum)
        keyword(EXCLUSIVE_MINIMUM, FROM_06, ValidationRules::exclusiveMinimum)
        keyword(EXCLUSIVE_MAXIMUM, BOOLEAN_EXCLUSIVE, named(EXCLUSIVE_MAXIMUM, ValidationRules::flag))
        keyword(EXCLUSIVE_MINIMUM, BOOLEAN_EXCLUSIVE, named(EXCLUSIVE_MINIMUM, ValidationRules::flag))
        count(Counted(name = "maxLength", measure = Measure.Length, isMaximum = true))
        count(Counted(name = "minLength", measure = Measure.Length, isMaximum = false))
        count(Counted(name = "maxItems", measure = Measure.Items, isMaximum = true))
        count(Counted(name = "minItems", measure = Measure.Items, isMaximum = false))
        count(Counted(name = "maxProperties", measure = Measure.Properties, isMaximum = true))
        count(Counted(name = "minProperties", measure = Measure.Properties, isMaximum = false))
        keyword("pattern", ALL, ValidationRules::pattern)
        keyword("uniqueItems", ALL, ValidationRules::uniqueItems)
        keyword("required", ALL, ValidationRules::required)
        keyword("dependentRequired", MODERN, ValidationRules::dependentRequired)
        keyword("minContains", MODERN, named("minContains", ValidationRules::countOnly))
        keyword("maxContains", MODERN, named("maxContains", ValidationRules::countOnly))
    }

    private fun RuleScope.unbound() {
        map("definitions", JSON)
        dependencies("dependencies", UNTIL_07, ApplicatorRules::dependencies)
        keyword("format", ALL, ValidationRules::format)
        keyword("nullable", setOf(Draft.OpenApi30), named("nullable", ValidationRules::flag))
    }

    private fun named(
        name: String,
        compile: (KeywordContext, JsonElement, String) -> Keyword?,
    ): (KeywordContext, JsonElement) -> Keyword? = { context, element -> compile(context, element, name) }

    private fun contextual(compile: (KeywordContext) -> Keyword): (KeywordContext, JsonElement) -> Keyword? =
        { context, _ -> compile(context) }

    private fun RuleScope.count(counted: Counted) {
        keyword(counted.name, ALL) { context, element -> ValidationRules.count(context, element, counted) }
    }
}
