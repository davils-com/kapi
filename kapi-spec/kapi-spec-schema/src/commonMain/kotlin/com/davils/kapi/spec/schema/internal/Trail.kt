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

package com.davils.kapi.spec.schema.internal

import com.davils.kaml.document.pointer.JsonPointer

internal class Trail(val parent: Trail?, val token: String) {
    fun tokens(): List<String> {
        val collected = ArrayList<String>()
        var current: Trail? = this
        while (current != null) {
            collected += current.token
            current = current.parent
        }
        collected.reverse()
        return collected
    }

    companion object {
        fun pointerOf(trail: Trail?): JsonPointer {
            val tokens = trail?.tokens() ?: emptyList()
            return JsonPointer.of(tokens)
        }
    }
}
