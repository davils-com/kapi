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

package com.davils.kapi.runtime.core.internal

private const val COMMA: Char = ','

internal object FieldLists {
    fun <T : Any> read(text: String, maxLength: Int, maxMembers: Int, element: (FieldScanner) -> T?): List<T>? {
        if (text.length > maxLength) return null
        return read(FieldScanner(text), maxMembers, element)
    }

    fun <T : Any> read(scanner: FieldScanner, maxMembers: Int, element: (FieldScanner) -> T?): List<T>? {
        val items = ArrayList<T>()
        var isValid = true
        while (isValid && hasNext(scanner)) {
            val item = element(scanner)
            isValid = item != null && items.size < maxMembers && endsElement(scanner)
            item?.let(items::add)
        }
        return items.takeIf { isValid }
    }

    private fun hasNext(scanner: FieldScanner): Boolean {
        scanner.skipWhitespace()
        while (scanner.consume(COMMA)) scanner.skipWhitespace()
        return !scanner.isAtEnd
    }

    private fun endsElement(scanner: FieldScanner): Boolean {
        scanner.skipWhitespace()
        return scanner.isAtEnd || scanner.consume(COMMA)
    }
}
