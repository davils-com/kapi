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

package com.davils.kapi.runtime.core.codec.internal

import com.davils.kapi.runtime.core.codec.Codec
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.codec.form.FormLimits
import com.davils.kapi.runtime.core.codec.form.FormUrlEncoding
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.outcome.Outcome
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val NOT_FLAT: String = "application/x-www-form-urlencoded carries an object of texts, numbers, flags " +
    "and lists of them"

internal class FormCodec(private val json: Json, private val limits: FormLimits) : Codec {
    private val trees: JsonCodec = JsonCodec(json, 1)

    override val mediaTypes: List<MediaRange> = listOf(MediaRange.of("application/x-www-form-urlencoded"))

    override fun <T> encode(serializer: SerializationStrategy<T>, value: T): ByteString {
        val members = json.encodeToJsonElement(serializer, value) as? JsonObject
        requireNotNull(members) { NOT_FLAT }
        val fields = members.flatMap { (name, member) -> fieldsOf(name, member) }
        return FormUrlEncoding.encode(fields).encodeToByteString()
    }

    override fun <T> decode(deserializer: DeserializationStrategy<T>, content: ByteString): Decoding<T> =
        when (val fields = FormUrlEncoding.decode(content.toByteArray(), limits)) {
            is Outcome.Failure -> Decoding.Failed("The form is refused: ${fields.failure}")
            is Outcome.Success -> trees.decodeTree(deserializer, treeOf(deserializer.descriptor, fields.value))
        }

    private fun fieldsOf(name: String, member: JsonElement): List<Pair<String, String>> = when (member) {
        is JsonNull -> emptyList()
        is JsonPrimitive -> listOf(name to member.content)
        is JsonArray -> member.map { item -> name to textOf(item) }
        is JsonObject -> throw IllegalArgumentException(NOT_FLAT)
    }

    private fun textOf(item: JsonElement): String {
        val primitive = item as? JsonPrimitive
        require(primitive != null && primitive != JsonNull) { NOT_FLAT }
        return primitive.content
    }

    @OptIn(DavilsInternal::class)
    private fun treeOf(descriptor: SerialDescriptor, fields: List<Pair<String, String>>): JsonObject {
        val grouped = OrderedMap<String, MutableList<JsonPrimitive>>()
        fields.forEach { (name, value) -> grouped.getOrPut(name) { ArrayList() } += JsonPrimitive(value) }
        val members = grouped.entries.map { (name, values) -> name to memberOf(descriptor, name, values) }
        return JsonObject(members.toMap())
    }

    private fun memberOf(descriptor: SerialDescriptor, name: String, values: List<JsonPrimitive>): JsonElement {
        val index = descriptor.getElementIndex(name)
        val isList = index >= 0 && descriptor.getElementDescriptor(index).kind == StructureKind.LIST
        if (isList || values.size > 1) return JsonArray(values)
        return values.first()
    }
}
