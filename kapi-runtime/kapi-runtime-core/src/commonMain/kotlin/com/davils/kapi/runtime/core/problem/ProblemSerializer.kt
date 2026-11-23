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

package com.davils.kapi.runtime.core.problem

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

private const val TYPE: String = "type"

private const val TITLE: String = "title"

private const val STATUS: String = "status"

private const val DETAIL: String = "detail"

private const val INSTANCE: String = "instance"

private const val CODE: String = "code"

private const val VIOLATIONS: String = "violations"

private const val FIRST_STATUS: Int = 100

private const val LAST_STATUS: Int = 599

private const val NOT_JSON: String = "A problem detail is written and read only by a JSON format"

private val violationList: KSerializer<List<ProblemViolation>> = ListSerializer(ProblemViolation.serializer())

/**
 * Writes a [Problem] as the JSON object of RFC 9457 and reads it back, keeping every extension member.
 *
 * A member left at its default, such as the type `about:blank` or an empty list of violations, is left out unless the
 * JSON format writes defaults. When a problem is read, a member whose value has the wrong type is ignored, a member
 * `violations` that is no list of complete violations is ignored, and every other member is kept as an extension.
 *
 * @since 1.0.0
 */
public object ProblemSerializer : KSerializer<Problem> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    /**
     * Writes [value] as a JSON object.
     *
     * @param encoder the encoder, which must be a JSON encoder.
     * @param value the problem.
     * @throws SerializationException if [encoder] belongs to no JSON format.
     * @since 1.0.0
     */
    override fun serialize(encoder: Encoder, value: Problem) {
        val json = encoder as? JsonEncoder ?: throw SerializationException(NOT_JSON)
        json.encodeJsonElement(treeOf(value, json.json))
    }

    /**
     * Reads a JSON object as a problem.
     *
     * @param decoder the decoder, which must be a JSON decoder.
     * @return the problem.
     * @throws SerializationException if [decoder] belongs to no JSON format or the value is no JSON object.
     * @since 1.0.0
     */
    override fun deserialize(decoder: Decoder): Problem {
        val json = decoder as? JsonDecoder ?: throw SerializationException(NOT_JSON)
        val members = json.decodeJsonElement() as? JsonObject ?: throw SerializationException("A problem is an object")
        return problemOf(members, json.json)
    }

    private fun treeOf(problem: Problem, json: Json): JsonObject {
        val isEager = json.configuration.encodeDefaults
        val members = LinkedHashMap<String, JsonElement>()
        if (isEager || problem.type != Problem.BLANK_TYPE) members[TYPE] = JsonPrimitive(problem.type)
        problem.title?.let { title -> members[TITLE] = JsonPrimitive(title) }
        problem.status?.let { status -> members[STATUS] = JsonPrimitive(status) }
        problem.detail?.let { detail -> members[DETAIL] = JsonPrimitive(detail) }
        problem.instance?.let { instance -> members[INSTANCE] = JsonPrimitive(instance) }
        problem.code?.let { code -> members[CODE] = JsonPrimitive(code) }
        if (isEager || problem.violations.isNotEmpty()) {
            members[VIOLATIONS] = json.encodeToJsonElement(violationList, problem.violations)
        }
        members.putAll(problem.extensions)
        return JsonObject(members)
    }

    private fun problemOf(members: JsonObject, json: Json): Problem = Problem(
        type = text(members, TYPE) ?: Problem.BLANK_TYPE,
        title = text(members, TITLE),
        status = statusOf(members),
        detail = text(members, DETAIL),
        instance = text(members, INSTANCE),
        code = text(members, CODE),
        violations = violationsOf(members[VIOLATIONS], json),
        extensions = members.filterKeys { name -> name !in Problem.MEMBERS },
    )

    private fun statusOf(members: JsonObject): Int? {
        val primitive = (members[STATUS] as? JsonPrimitive)?.takeUnless(JsonPrimitive::isString)
        return primitive?.intOrNull?.takeIf { status -> status in FIRST_STATUS..LAST_STATUS }
    }

    private fun text(members: JsonObject, name: String): String? {
        val primitive = members[name] as? JsonPrimitive
        return primitive?.takeIf(JsonPrimitive::isString)?.content
    }

    private fun violationsOf(member: JsonElement?, json: Json): List<ProblemViolation> {
        val items = member as? JsonArray ?: return emptyList()
        return try {
            json.decodeFromJsonElement(violationList, items)
        } catch (_: SerializationException) {
            emptyList()
        }
    }
}
