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
import com.davils.kapi.spec.schema.internal.Patterns
import com.davils.kapi.spec.schema.internal.Refusal
import com.davils.kapi.spec.schema.internal.Uris
import com.davils.kapi.spec.schema.internal.dialect.Draft
import com.davils.kapi.spec.schema.internal.dialect.Profile
import com.davils.kapi.spec.schema.internal.value.Canonical
import com.davils.kapi.spec.schema.internal.value.Decimal
import com.davils.kapi.spec.schema.internal.value.InstanceReader
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.violation.ViolationCode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class KeywordContext(
    val factory: NodeFactory,
    val session: Session,
    val target: Target,
    val members: JsonObject,
    val resource: Int,
    val profile: Profile,
) {
    val draft: Draft get() = profile.draft

    fun active(name: String): JsonElement? = members[name]?.takeIf { KeywordRules.ruleOf(name, profile) != null }

    fun child(vararg tokens: String): Int = factory.nodeAt(target.document, target.pointer + tokens)

    fun report(keyword: String, message: String, code: ViolationCode = DiagnosticCodes.KEYWORD_INVALID): Nothing? {
        session.report(code, message, Target(target.document, target.pointer + keyword))
        return null
    }

    fun schemaOrFlag(element: JsonElement, vararg tokens: String): Int {
        val isTrue = flagOf(element)?.takeIf { !draft.areBooleanSchemasAllowed } ?: return child(*tokens)
        val absolute = session.absoluteOf(resource, target.pointer + tokens)
        return factory.constant(isTrue, resource, absolute)
    }

    fun schemas(keyword: String, element: JsonElement): List<Int>? {
        val items = (element as? JsonArray)?.takeIf { it.isNotEmpty() }
            ?: return report(keyword, "The keyword ${SafeText.quoted(keyword)} must be a non-empty array of schemas")
        return items.indices.map { index -> child(keyword, index.toString()) }
    }

    fun schemaMap(keyword: String, element: JsonElement): List<Pair<String, Int>>? {
        val entries = element as? JsonObject
            ?: return report(keyword, "The keyword ${SafeText.quoted(keyword)} must be an object of schemas")
        return entries.keys.map { key -> key to child(keyword, key) }
    }

    fun text(keyword: String, element: JsonElement): String? =
        textOf(element) ?: report(keyword, "The keyword ${SafeText.quoted(keyword)} must be a string")

    fun texts(keyword: String, element: JsonElement): List<String>? {
        val array = element as? JsonArray
        val texts = array.orEmpty().mapNotNull { item -> textOf(item) }
        val isValid = array != null && texts.size == array.size
        return texts.takeIf { isValid }
            ?: report(keyword, "The keyword ${SafeText.quoted(keyword)} must be an array of strings")
    }

    fun flag(keyword: String, element: JsonElement): Boolean? =
        flagOf(element) ?: report(keyword, "The keyword ${SafeText.quoted(keyword)} must be a boolean")

    fun number(keyword: String, element: JsonElement): Decimal? {
        val primitive = (element as? JsonPrimitive)?.takeIf { value ->
            !value.isString && flagOf(value) == null && value.content != "null"
        }
        val number = primitive?.let { Decimal.parse(it.content) }
        return number ?: report(keyword, "The keyword ${SafeText.quoted(keyword)} must be a number")
    }

    fun count(keyword: String, element: JsonElement): Long? {
        val number = (element as? JsonPrimitive)?.takeIf { !it.isString }?.let { Decimal.parse(it.content) }
        val isCount = number != null && number.isInteger && number.sign >= 0
        val value = number?.takeIf { isCount }?.let(::longOf)
        return value ?: report(keyword, "The keyword ${SafeText.quoted(keyword)} must be a non-negative integer")
    }

    fun literal(keyword: String, element: JsonElement): String? {
        val instance = runCatching { InstanceReader(session.limits.maximumSchemaDepth).read(element) }
        val failure = instance.exceptionOrNull() as? Refusal
        failure?.let { report(keyword, it.description) }
        return instance.getOrNull()?.let { value -> Canonical.of(value) { } }
    }

    fun pattern(keyword: String, source: String): ((CharSequence) -> Boolean)? {
        val maximum = session.limits.maximumPatternLength
        if (source.length > maximum) {
            return report(keyword, "The pattern is longer than $maximum characters", DiagnosticCodes.LIMIT_EXCEEDED)
        }
        val compiled = Patterns.compile(
            source = source,
            maximumSourceLength = maximum,
            maximumInputLength = session.limits.maximumStringLength,
        )
        return compiled
            ?: report(
                keyword,
                "The pattern ${SafeText.quoted(source)} is not supported",
                DiagnosticCodes.PATTERN_REFUSED,
            )
    }

    fun reference(keyword: String, element: JsonElement): Reference? {
        val text = text(keyword, element) ?: return null
        return resolved(keyword, text)
    }

    private fun resolved(keyword: String, text: String): Reference? {
        val base = session.resources[resource].uri
        val absolute = Uris.resolve(text, base)
        val located = absolute?.let(session::locate)
        val reference = located?.let { Reference(factory.nodeAt(it.document, it.pointer), absolute, it) }
        return reference ?: report(
            keyword,
            "The reference ${SafeText.quoted(text)} leads to no schema that is known or could be loaded",
            DiagnosticCodes.REFERENCE_UNRESOLVED,
        )
    }

    private fun textOf(element: JsonElement): String? = (element as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun flagOf(element: JsonElement): Boolean? {
        val primitive = (element as? JsonPrimitive)?.takeIf { !it.isString }
        val content = primitive?.content
        val isFlag = content == "true" || content == "false"
        return (content == "true").takeIf { isFlag }
    }

    private fun longOf(number: Decimal): Long {
        val isSmall = number.digits.length + number.exponent <= MAXIMUM_LONG_DIGITS
        val zeros = "0".repeat(number.exponent.toInt().coerceAtLeast(0))
        val text = (number.digits.ifEmpty { "0" } + zeros).takeIf { isSmall }
        return text?.toLongOrNull() ?: Long.MAX_VALUE
    }

    private companion object {
        const val MAXIMUM_LONG_DIGITS: Int = 18
    }
}
