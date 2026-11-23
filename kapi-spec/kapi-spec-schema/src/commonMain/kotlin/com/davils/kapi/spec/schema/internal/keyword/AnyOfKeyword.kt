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

internal class AnyOfKeyword(private val children: List<Int>) : Keyword("anyOf") {
    override fun evaluate(frame: Frame): Boolean {
        val failures = frame.temporarySink()
        var isMatched = false
        for ((index, child) in children.withIndex()) {
            if (isMatched && !frame.run.isTrackingAnnotations) break
            val result = frame.inPlace(
                index = child,
                keywordTrail = frame.trail(name, index.toString()),
                sink = failures,
            )
            frame.merge(result)
            isMatched = isMatched || result != null
        }
        if (!isMatched) frame.failAll(failures)
        return isMatched || failed(frame, "The value matches none of the ${children.size} subschemas")
    }
}
