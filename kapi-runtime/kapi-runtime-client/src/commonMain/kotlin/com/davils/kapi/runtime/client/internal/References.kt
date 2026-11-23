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

package com.davils.kapi.runtime.client.internal

import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom

internal object References {
    fun resolve(base: String, reference: String): String {
        val target = reference.substringBefore('#')
        val whole = base.substringBefore('#')
        val plain = whole.substringBefore('?')
        val kept = whole.takeIf { target.isEmpty() }
        val queried = (plain + target).takeIf { target.startsWith('?') }
        return kept ?: queried ?: built(plain, target)
    }

    private fun built(base: String, target: String): String {
        val builder = URLBuilder(base)
        builder.takeFrom(target)
        builder.fragment = ""
        return builder.buildString()
    }
}
