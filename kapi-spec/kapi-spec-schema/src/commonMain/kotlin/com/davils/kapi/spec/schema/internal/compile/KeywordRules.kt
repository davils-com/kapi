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
        core()
        applicators()
        validations()
    }

    private val byName: Map<String, List<KeywordRule>> = rules.groupBy { it.name }

    fun ruleOf(name: String, profile: Profile): KeywordRule? {
        val candidates = byName[name] ?: return null
        return candidates.firstOrNull { rule -> profile.draft in rule.drafts && profile.enables(rule.vocabulary) }
    }

    private fun MutableList<KeywordRule>.core() {
        rule("\$ref", ALL, Vocabulary.Core, compile = CoreRules::ref)
        rule("\$dynamicRef", setOf(Draft.Draft202012), Vocabulary.Core, compile = CoreRules::dynamicRef)
        rule("\$recursiveRef", setOf(Draft.Draft201909), Vocabulary.Core, compile = CoreRules::recursiveRef)
        rule("\$defs", MODERN, Vocabulary.Core, Shape.Map)
        rule("definitions", JSON, null, Shape.Map)
    }

    private fun MutableList<KeywordRule>.applicators() {
        val applicator = Vocabulary.Applicator
        rule("allOf", ALL, applicator, Shape.List, ApplicatorRules::allOf)
        rule("anyOf", ALL, applicator, Shape.List, ApplicatorRules::anyOf)
        rule("oneOf", ALL, applicator, Shape.List, ApplicatorRules::oneOf)
        rule("not", ALL, applicator, Shape.Single, contextual(ApplicatorRules::not))
        rule("if", FROM_07, applicator, Shape.Single, contextual(ApplicatorRules::conditional))
        rule("then", FROM_07, applicator, Shape.Single)
        rule("else", FROM_07, applicator, Shape.Single)
        rule("properties", ALL, applicator, Shape.Map, ApplicatorRules::properties)
        rule("patternProperties", JSON, applicator, Shape.Map, ApplicatorRules::patternProperties)
        rule("additionalProperties", ALL, applicator, Shape.Single, ApplicatorRules::additionalProperties)
        rule("propertyNames", FROM_06, applicator, Shape.Single, contextual(ApplicatorRules::propertyNames))
        rule("dependentSchemas", MODERN, applicator, Shape.Map, ApplicatorRules::dependentSchemas)
        rule("dependencies", UNTIL_07, null, Shape.Dependencies, ApplicatorRules::dependencies)
        rule("items", setOf(Draft.Draft202012, Draft.OpenApi30), applicator, Shape.Single, ApplicatorRules::items)
        rule("items", UNTIL_2019, applicator, Shape.SingleOrList, ApplicatorRules::legacyItems)
        rule("prefixItems", setOf(Draft.Draft202012), applicator, Shape.List, ApplicatorRules::prefixItems)
        rule("additionalItems", UNTIL_2019, applicator, Shape.Single, ApplicatorRules::additionalItems)
        rule("contains", FROM_06, applicator, Shape.Single, contextual(ApplicatorRules::contains))
        val unevaluated = Vocabulary.Unevaluated
        rule("unevaluatedItems", MODERN, unevaluated, Shape.Single, contextual(ApplicatorRules::unevaluatedItems))
        val properties = contextual(ApplicatorRules::unevaluatedProperties)
        rule("unevaluatedProperties", MODERN, unevaluated, Shape.Single, properties)
    }

    private fun MutableList<KeywordRule>.validations() {
        val validation = Vocabulary.Validation
        rule("type", ALL, validation, compile = ValidationRules::type)
        rule("enum", ALL, validation, compile = ValidationRules::enumeration)
        rule("const", FROM_06, validation, compile = ValidationRules::constant)
        rule("multipleOf", ALL, validation, compile = ValidationRules::multipleOf)
        rule("maximum", ALL, validation, compile = ValidationRules::maximum)
        rule("minimum", ALL, validation, compile = ValidationRules::minimum)
        rule(EXCLUSIVE_MAXIMUM, FROM_06, validation, compile = ValidationRules::exclusiveMaximum)
        rule(EXCLUSIVE_MINIMUM, FROM_06, validation, compile = ValidationRules::exclusiveMinimum)
        val flag = ValidationRules::flag
        rule(EXCLUSIVE_MAXIMUM, BOOLEAN_EXCLUSIVE, validation, compile = named(EXCLUSIVE_MAXIMUM, flag))
        rule(EXCLUSIVE_MINIMUM, BOOLEAN_EXCLUSIVE, validation, compile = named(EXCLUSIVE_MINIMUM, flag))
        count("maxLength", Measure.Length, isMaximum = true)
        count("minLength", Measure.Length, isMaximum = false)
        count("maxItems", Measure.Items, isMaximum = true)
        count("minItems", Measure.Items, isMaximum = false)
        count("maxProperties", Measure.Properties, isMaximum = true)
        count("minProperties", Measure.Properties, isMaximum = false)
        rule("pattern", ALL, validation, compile = ValidationRules::pattern)
        rule("uniqueItems", ALL, validation, compile = ValidationRules::uniqueItems)
        rule("required", ALL, validation, compile = ValidationRules::required)
        rule("dependentRequired", MODERN, validation, compile = ValidationRules::dependentRequired)
        rule("minContains", MODERN, validation, compile = named("minContains", ValidationRules::countOnly))
        rule("maxContains", MODERN, validation, compile = named("maxContains", ValidationRules::countOnly))
        rule("format", ALL, null, compile = ValidationRules::format)
        rule("nullable", setOf(Draft.OpenApi30), null, compile = named("nullable", ValidationRules::flag))
    }

    private fun named(
        name: String,
        compile: (KeywordContext, JsonElement, String) -> Keyword?,
    ): (KeywordContext, JsonElement) -> Keyword? = { context, element -> compile(context, element, name) }

    private fun contextual(compile: (KeywordContext) -> Keyword): (KeywordContext, JsonElement) -> Keyword? =
        { context, _ -> compile(context) }

    private fun MutableList<KeywordRule>.count(name: String, measure: Measure, isMaximum: Boolean) {
        val compile: (KeywordContext, JsonElement) -> Keyword? = { context, element ->
            ValidationRules.count(context, element, Counted(name, measure, isMaximum))
        }
        rule(name, ALL, Vocabulary.Validation, compile = compile)
    }

    private fun MutableList<KeywordRule>.rule(
        name: String,
        drafts: Set<Draft>,
        vocabulary: Vocabulary?,
        shape: Shape = Shape.None,
        compile: ((KeywordContext, JsonElement) -> Keyword?)? = null,
    ) {
        add(KeywordRule(name, drafts, vocabulary, shape, compile))
    }
}
