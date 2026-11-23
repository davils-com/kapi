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

package com.davils.kapi.runtime.core.structured.internal

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.Dictionary
import com.davils.kapi.runtime.core.structured.InnerList
import com.davils.kapi.runtime.core.structured.Item
import com.davils.kapi.runtime.core.structured.Member
import com.davils.kapi.runtime.core.structured.Parameters
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

private const val SPACE: Char = ' '

private const val ASCII_LIMIT: Char = '\u0080'

private val truth: BareItem = BareItem.Flag(true)

internal class StructuredParser(private val text: String, private val limits: FieldLimits) {
    private val cursor: StructuredCursor = StructuredCursor(text)

    private val bareItems: BareItemReader = BareItemReader(cursor)

    fun parseItem(): Item? = whole(::item)

    fun parseList(): List<Member>? = whole(::listMembers)

    fun parseDictionary(): Dictionary? = whole(::dictionaryEntries)

    private fun <T : Any> whole(read: () -> T): T? {
        val isReadable = text.length <= limits.maxFieldLength && text.all { char -> char < ASCII_LIMIT }
        if (!isReadable) return null
        cursor.skipSpaces()
        val value = read()
        cursor.skipSpaces()
        return value.takeIf { !cursor.isFailed && cursor.isAtEnd }
    }

    private fun listMembers(): List<Member> {
        val members = ArrayList<Member>()
        while (!cursor.isFailed && !cursor.isAtEnd) {
            members += member()
            if (members.size > limits.maxMembers) cursor.fail()
            separator()
        }
        return members
    }

    private fun dictionaryEntries(): Dictionary {
        val entries = OrderedMap<String, Member>()
        while (!cursor.isFailed && !cursor.isAtEnd) {
            val key = key()
            val member = dictionaryMember()
            if (!cursor.isFailed) entries[key] = member
            if (entries.size > limits.maxMembers) cursor.fail()
            separator()
        }
        return Dictionary(entries.entries.takeUnless { cursor.isFailed }.orEmpty())
    }

    private fun dictionaryMember(): Member {
        if (cursor.consume('=')) return member()
        return Item(truth, parameters())
    }

    private fun separator() {
        cursor.skipWhitespace()
        if (cursor.isAtEnd) return
        if (!cursor.consume(',')) cursor.fail()
        cursor.skipWhitespace()
        if (cursor.isAtEnd) cursor.fail()
    }

    private fun member(): Member {
        if (cursor.peek() == '(') return innerList()
        return item()
    }

    private fun innerList(): InnerList {
        cursor.skip(1)
        val items = ArrayList<Item>()
        var isOpen = true
        while (isOpen && !cursor.isFailed) {
            cursor.skipSpaces()
            isOpen = !cursor.consume(')')
            if (isOpen) innerItem(items)
        }
        return InnerList(items, parameters())
    }

    private fun innerItem(items: MutableList<Item>) {
        if (cursor.isAtEnd) {
            cursor.fail()
            return
        }
        items += item()
        val next = cursor.peek()
        if (items.size > limits.maxMembers || (next != SPACE && next != ')')) cursor.fail()
    }

    private fun item(): Item {
        val value = bareItems.read()
        return Item(value, parameters())
    }

    private fun parameters(): Parameters {
        val entries = OrderedMap<String, BareItem>()
        while (!cursor.isFailed && cursor.consume(';')) {
            cursor.skipSpaces()
            val key = key()
            val value = valueOrTrue(cursor.consume('='))
            if (!cursor.isFailed) entries[key] = value
            if (entries.size > limits.maxParameters) cursor.fail()
        }
        if (cursor.isFailed) return Parameters.Empty
        return Parameters(entries.entries)
    }

    private fun valueOrTrue(hasValue: Boolean): BareItem {
        if (!hasValue) return truth
        return bareItems.read()
    }

    private fun key(): String {
        val first = cursor.peek()
        if (first == null || !StructuredGrammar.isKeyStart(first)) cursor.fail()
        return cursor.span(StructuredGrammar::isKeyChar)
    }
}
