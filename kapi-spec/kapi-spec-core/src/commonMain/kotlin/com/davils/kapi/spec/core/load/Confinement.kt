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

package com.davils.kapi.spec.core.load

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.policy.ReferencePolicy

private const val SEPARATOR: Char = '/'

private const val PARENT: String = ".."

private val forbidden: List<String> = listOf("%2e", "%2f", "%5c", "%00", "\\")

internal class Confinement(
    private val own: DocumentUri,
    private val root: String,
    private val isExternalAllowed: Boolean,
) {
    fun contains(uri: DocumentUri): Boolean {
        val text = uri.value
        val isInside = isExternalAllowed && text.startsWith(root) && !isEscaping(text)
        return uri == own || isInside
    }

    private fun isEscaping(text: String): Boolean {
        val lowered = text.lowercase()
        val hasEncoded = forbidden.any { sequence -> sequence in lowered }
        val hasParent = text.split(SEPARATOR).any { segment -> segment == PARENT }
        return hasEncoded || hasParent
    }

    companion object {
        fun of(policy: ReferencePolicy, own: DocumentUri): Confinement {
            val folder = own.value.substringBeforeLast(SEPARATOR) + SEPARATOR
            val root = policy.root?.value ?: folder
            return Confinement(own, root, policy.isExternalAllowed)
        }
    }
}
