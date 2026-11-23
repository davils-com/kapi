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

internal class UnevaluatedItemsKeyword(private val child: Int) : Keyword("unevaluatedItems") {
    override val isUnevaluated: Boolean get() = true

    override fun evaluate(frame: Frame): Boolean {
        val items = (frame.instance as? Instance.ArrayValue)?.items ?: return true
        var isValid = true
        for ((index, item) in items.withIndex()) {
            val isPassed = frame.evaluated.isMarked(index) || checked(frame, index, item)
            isValid = isValid && isPassed
            if (!isValid && !frame.isCollecting) break
        }
        return isValid
    }

    private fun checked(frame: Frame, index: Int, item: Instance): Boolean {
        frame.evaluated.mark(index)
        return frame.descend(
            index = child,
            child = item,
            token = index.toString(),
            keywordTrail = frame.trail(name),
            sink = frame.sink,
        )
    }
}
