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

internal class ContainsKeyword(
    private val child: Int,
    private val minimum: Long,
    private val maximum: Long?,
    private val minimumName: String,
    private val isMarking: Boolean,
) : Keyword("contains") {
    override fun evaluate(frame: Frame): Boolean {
        val items = (frame.instance as? Instance.ArrayValue)?.items ?: return true
        val count = matches(frame, items)
        val hasMinimum = count >= minimum ||
            frame.fail(keyword = minimumName, message = "The array has $count matching items but needs $minimum")
        val hasMaximum = maximum == null ||
            count <= maximum ||
            frame.fail(
                keyword = "maxContains",
                message = "The array has $count matching items but allows at most $maximum",
            )
        return hasMinimum && hasMaximum
    }

    private fun matches(frame: Frame, items: List<Instance>): Long {
        val isCountingAll = maximum != null || (isMarking && frame.run.isTrackingAnnotations)
        var count = 0L
        var index = 0
        while (index < items.size && (isCountingAll || count < minimum)) {
            val isPassed = frame.descend(
                index = child,
                child = items[index],
                token = index.toString(),
                keywordTrail = frame.trail(name),
                sink = null,
            )
            if (isPassed && isMarking) frame.evaluated.mark(index)
            count += 1L.takeIf { isPassed } ?: 0L
            index++
        }
        return count
    }
}
