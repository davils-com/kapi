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

import com.davils.kapi.spec.schema.internal.evaluate.Evaluated
import com.davils.kapi.spec.schema.internal.evaluate.Frame
import com.davils.kapi.spec.schema.internal.evaluate.Keyword

internal class OneOfKeyword(private val children: List<Int>) : Keyword("oneOf") {
    override fun evaluate(frame: Frame): Boolean {
        val failures = frame.temporarySink()
        val matched = ArrayList<Int>()
        var first: Evaluated? = null
        for ((index, child) in children.withIndex()) {
            val result = frame.inPlace(
                index = child,
                keywordTrail = frame.trail(name, index.toString()),
                sink = failures,
            )
            first = first ?: result
            if (result != null) matched += index
            if (matched.size > 1) break
        }
        if (matched.size == 1) frame.merge(first)
        if (matched.isEmpty()) frame.failAll(failures)
        val message = "The value matches none of the ${children.size} subschemas".takeIf { matched.isEmpty() }
            ?: "The value matches the subschemas ${matched.joinToString(" and ")} but must match exactly one"
        return matched.size == 1 || failed(frame, message)
    }
}
