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

import kotlinx.io.bytestring.ByteString

private const val BYTE_ORDER_MARK: Int = 0xFEFF

internal object SourceText {
    fun decode(content: ByteString): String? {
        val text = strict(content.toByteArray()) ?: return null
        return text.removePrefix(Char(BYTE_ORDER_MARK).toString())
    }

    private fun strict(bytes: ByteArray): String? = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        null
    }
}
