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

package com.davils.kapi.spec.openapi.load

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.openapi.policy.ReferencePolicy

private const val SEPARATOR: Char = '/'

private val escapes: Regex = Regex("""(^|/)\.\.(/|$)|%2[eEfF]|%5[cC]|%00|\\""")

internal class Confinement(
    private val own: DocumentUri,
    private val root: String,
    private val isExternalAllowed: Boolean,
) {
    fun contains(uri: DocumentUri): Boolean {
        val isInside = isExternalAllowed && uri.value.startsWith(root) && !escapes.containsMatchIn(uri.value)
        return uri == own || isInside
    }

    companion object {
        fun of(policy: ReferencePolicy, own: DocumentUri): Confinement {
            val folder = own.value.substringBeforeLast(SEPARATOR) + SEPARATOR
            val root = policy.root?.value ?: folder
            return Confinement(own, root, policy.isExternalAllowed)
        }
    }
}
