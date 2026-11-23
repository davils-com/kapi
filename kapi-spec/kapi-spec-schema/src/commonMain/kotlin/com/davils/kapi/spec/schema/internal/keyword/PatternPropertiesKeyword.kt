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

package com.davils.kapi.spec.schema.internal.keyword

import com.davils.kapi.spec.schema.internal.evaluate.Frame
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import com.davils.kapi.spec.schema.internal.value.Instance

internal class PatternPropertiesKeyword(private val entries: List<PatternEntry>) : Keyword("patternProperties") {
    override fun evaluate(frame: Frame): Boolean {
        val members = frame.instance as? Instance.ObjectValue ?: return true
        var isValid = true
        for ((index, property) in members.keys.withIndex()) {
            val isPassed = entries.all { entry ->
                check(frame = frame, members = members, index = index, property = property, entry = entry)
            }
            isValid = isValid && isPassed
            if (!isValid && !frame.isCollecting) break
        }
        return isValid
    }

    private fun check(
        frame: Frame,
        members: Instance.ObjectValue,
        index: Int,
        property: String,
        entry: PatternEntry,
    ): Boolean {
        if (!Patterns.matches(entry.matcher, property, frame.run)) return true
        frame.evaluated.mark(index)
        val trail = frame.trail(name, entry.source)
        return frame.descend(
            index = entry.schema,
            child = members.values[index],
            token = property,
            keywordTrail = trail,
            sink = frame.sink,
        )
    }
}
