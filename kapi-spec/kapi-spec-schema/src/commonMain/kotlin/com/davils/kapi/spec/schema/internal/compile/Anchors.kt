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

import com.davils.kapi.spec.schema.internal.dialect.Draft

internal object Anchors {
    fun isValid(name: String, draft: Draft): Boolean {
        val first = name.firstOrNull() ?: return false
        val isModern = draft == Draft.Draft202012
        val startsWell = first.isAsciiLetter() || (isModern && first == '_')
        val extra = "-._".takeIf { isModern } ?: "-.:_"
        return startsWell && name.all { it.isAsciiLetter() || it in '0'..'9' || it in extra }
    }

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
}
