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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object Literals {
    fun count(element: JsonElement, maximum: Long): Long {
        val stack = ArrayDeque<JsonElement>()
        stack.addLast(element)
        var count = 0L
        while (stack.isNotEmpty() && count <= maximum) {
            val current = stack.removeLast()
            count++
            (current as? JsonArray)?.forEach(stack::addLast)
            (current as? JsonObject)?.run { values.forEach(stack::addLast) }
        }
        return count
    }
}
