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

package com.davils.kapi.spec.schema.internal.keyword

import com.davils.kapi.spec.schema.internal.evaluate.Frame
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import com.davils.kapi.spec.schema.internal.value.Instance
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

internal class DependenciesKeyword(name: String, private val entries: List<Dependency>) : Keyword(name) {
    override fun evaluate(frame: Frame): Boolean {
        val members = frame.instance as? Instance.ObjectValue ?: return true
        var isValid = true
        for (entry in entries) {
            val isPresent = members.indexOf(entry.property) != null
            val passed = !isPresent || satisfies(frame, members, entry)
            isValid = isValid && passed
            if (!isValid && !frame.collects) break
        }
        return isValid
    }

    private fun satisfies(frame: Frame, members: Instance.ObjectValue, entry: Dependency): Boolean {
        val required = entry.required.orEmpty()
        val missing = required.filter { property -> members.indexOf(property) == null }
        missing.forEach { property ->
            val message = "The property ${SafeText.quoted(property)} is missing, which " +
                "${SafeText.quoted(entry.property)} requires"
            frame.fail(name, message)
        }
        val schema = entry.schema ?: return missing.isEmpty()
        val result = frame.inPlace(schema, frame.trail(name, entry.property), frame.sink)
        frame.merge(result)
        return missing.isEmpty() && result != null
    }
}
