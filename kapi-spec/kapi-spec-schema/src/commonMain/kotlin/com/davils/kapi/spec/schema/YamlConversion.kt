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

package com.davils.kapi.spec.schema

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.json.JsonElement

private const val HASH_FACTOR: Int = 31

/**
 * The result of converting a YAML node into JSON with [JsonSchema.json].
 *
 * @since 1.0.0
 */
public sealed interface YamlConversion {
    /**
     * The node converted.
     *
     * @property json the JSON value of the node.
     * @since 1.0.0
     */
    public class Converted internal constructor(public val json: JsonElement) : YamlConversion {
        override fun equals(other: Any?): Boolean = other is Converted && json == other.json

        override fun hashCode(): Int = json.hashCode()

        override fun toString(): String = "Converted"
    }

    /**
     * The node holds something JSON cannot express or exceeds a limit.
     *
     * @property pointer the place of the node that could not be converted.
     * @property reason why, in English; text of the node in it is escaped.
     * @since 1.0.0
     */
    public class Refused internal constructor(public val pointer: JsonPointer, public val reason: String) :
        YamlConversion {
        override fun equals(other: Any?): Boolean =
            other is Refused && pointer == other.pointer && reason == other.reason

        override fun hashCode(): Int = pointer.hashCode() * HASH_FACTOR + reason.hashCode()

        override fun toString(): String = "Refused(${SafeText.text(pointer.text)}: $reason)"
    }
}
