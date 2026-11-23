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

internal class RequiredKeyword(private val names: List<String>) : Keyword("required") {
    override fun evaluate(frame: Frame): Boolean {
        val members = frame.instance as? Instance.ObjectValue ?: return true
        frame.run.charge(names.size.toLong())
        val missing = names.filter { property -> members.indexOf(property) == null }
        missing.forEach { property -> failed(frame, "The property ${SafeText.quoted(property)} is missing") }
        return missing.isEmpty()
    }
}
