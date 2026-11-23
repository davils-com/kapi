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
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.Utf8
import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.Dictionary
import com.davils.kapi.runtime.core.structured.InnerList
import com.davils.kapi.runtime.core.structured.Item
import com.davils.kapi.runtime.core.structured.Member
import com.davils.kapi.runtime.core.structured.Parameters
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.ByteStringBuilder
import kotlin.io.encoding.Base64

private const val SPACE: Char = ' '

private const val MOST_INTEGER_DIGITS: Int = 15

private const val MOST_WHOLE_DIGITS: Int = 12

private const val MOST_FRACTION_DIGITS: Int = 3

private const val SCALE: Long = 1_000L

private const val RADIX: Int = 16

private const val BASE64_SYMBOLS: String = "+/="

private const val ASCII_LIMIT: Char = '\u0080'

private val placeholder: Item = Item(BareItem.Flag(false))

private val truth: BareItem = BareItem.Flag(true)

private val base64: Base64 = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

internal class StructuredParser(private val text: String, private val limits: FieldLimits) {
    private var position: Int = 0

    private var isFailed: Boolean = false

    private val readers: Map<Char, () -> BareItem> = buildMap {
        ('0'..'9').forEach { digit -> put(digit, ::number) }
        put('-', ::number)
        (('a'..'z') + ('A'..'Z') + '*').forEach { letter -> put(letter, ::token) }
        put('"', ::string)
        put(':', ::bytes)
        put('?', ::flag)
        put('@', ::date)
        put('%', ::displayString)
    }

    fun parseItem(): Item? = whole(::item)

    fun parseList(): List<Member>? = whole(::listMembers)

    fun parseDictionary(): Dictionary? = whole(::dictionaryEntries)

    private fun <T : Any> whole(read: () -> T): T? {
        val isReadable = text.length <= limits.maxFieldLength && text.all { char -> char < ASCII_LIMIT }
        if (!isReadable) return null
        skipSpaces()
        val value = read()
        skipSpaces()
        return value.takeIf { !isFailed && position == text.length }
    }

    private fun listMembers(): List<Member> {
        val members = ArrayList<Member>()
        while (!isFailed && position < text.length) {
            members += member()
            if (members.size > limits.maxMembers) fail()
            separator()
        }
        return members
    }

    private fun dictionaryEntries(): Dictionary {
        val entries = OrderedMap<String, Member>()
        while (!isFailed && position < text.length) {
            val key = key()
            val member = dictionaryMember()
            if (!isFailed) entries[key] = member
            if (entries.size > limits.maxMembers) fail()
            separator()
        }
        return Dictionary(entries.entries.takeUnless { isFailed }.orEmpty())
    }

    private fun dictionaryMember(): Member {
        if (consume('=')) return member()
        return Item(truth, parameters())
    }

    private fun separator() {
        skipWhitespace()
        if (position >= text.length) return
        if (!consume(',')) fail()
        skipWhitespace()
        if (position >= text.length) fail()
    }

    private fun member(): Member {
        if (peek() == '(') return innerList()
        return item()
    }

    private fun innerList(): InnerList {
        position += 1
        val items = ArrayList<Item>()
        var isOpen = true
        while (isOpen && !isFailed) {
            skipSpaces()
            isOpen = !consume(')')
            if (isOpen) innerItem(items)
        }
        return InnerList(items, parameters())
    }

    private fun innerItem(items: MutableList<Item>) {
        if (position >= text.length) {
            fail()
            return
        }
        items += item()
        val next = peek()
        if (items.size > limits.maxMembers || (next != SPACE && next != ')')) fail()
    }

    private fun item(): Item {
        val value = bareItem()
        return Item(value, parameters())
    }

    private fun parameters(): Parameters {
        val entries = OrderedMap<String, BareItem>()
        while (!isFailed && consume(';')) {
            skipSpaces()
            val key = key()
            val value = valueOrTrue(consume('='))
            if (!isFailed) entries[key] = value
            if (entries.size > limits.maxParameters) fail()
        }
        if (isFailed) return Parameters.Empty
        return Parameters(entries.entries)
    }

    private fun valueOrTrue(hasValue: Boolean): BareItem {
        if (!hasValue) return truth
        return bareItem()
    }

    private fun key(): String {
        val first = peek()
        if (first == null || !StructuredGrammar.isKeyStart(first)) fail()
        return span(StructuredGrammar::isKeyChar)
    }

    private fun bareItem(): BareItem {
        val reader = peek()?.let(readers::get)
        return reader?.invoke() ?: failed()
    }

    private fun number(): BareItem {
        val isNegative = consume('-')
        val whole = span(FieldGrammar::isDigit)
        val hasPoint = consume('.')
        val fraction = span(FieldGrammar::isDigit).takeIf { hasPoint }.orEmpty()
        val isValid = whole.isNotEmpty() && (hasPoint == fraction.isNotEmpty())
        val isBounded = isValid && whole.length <= MOST_INTEGER_DIGITS && isDecimalBounded(hasPoint, whole, fraction)
        if (!isBounded) return failed()
        val sign = (-1L).takeIf { isNegative } ?: 1L
        return numberOf(sign, whole, fraction.takeIf { hasPoint })
    }

    private fun isDecimalBounded(hasPoint: Boolean, whole: String, fraction: String): Boolean =
        !hasPoint || (whole.length <= MOST_WHOLE_DIGITS && fraction.length <= MOST_FRACTION_DIGITS)

    private fun numberOf(sign: Long, whole: String, fraction: String?): BareItem {
        if (fraction == null) return BareItem.Integer(sign * whole.toLong())
        val thousandths = whole.toLong() * SCALE + fraction.padEnd(MOST_FRACTION_DIGITS, '0').toLong()
        return BareItem.Decimal(sign * thousandths)
    }

    private fun string(): BareItem {
        position += 1
        val out = StringBuilder()
        var isOpen = true
        while (isOpen && !isFailed) {
            val char = next()
            isOpen = char != '"'
            if (isOpen) stringChar(out, char)
        }
        return BareItem.Text(out.toString().takeUnless { isFailed }.orEmpty())
    }

    private fun stringChar(out: StringBuilder, char: Char?) {
        val shown = escapedOf(char)
        if (shown == null || !StructuredGrammar.isPrintable(shown)) fail()
        shown?.let(out::append)
    }

    private fun escapedOf(char: Char?): Char? {
        if (char != '\\') return char
        return next()?.takeIf { found -> found == '"' || found == '\\' }
    }

    private fun token(): BareItem = BareItem.Token(span(StructuredGrammar::isTokenChar))

    private fun bytes(): BareItem {
        position += 1
        val encoded = span { char -> FieldGrammar.isAlphaNumeric(char) || char in BASE64_SYMBOLS }
        if (!consume(':')) fail()
        val decoded = try {
            base64.decode(encoded)
        } catch (_: IllegalArgumentException) {
            null
        }
        if (decoded == null) fail()
        return BareItem.Bytes(ByteString(decoded ?: ByteArray(0)))
    }

    private fun flag(): BareItem {
        position += 1
        val char = next()
        if (char != '0' && char != '1') fail()
        return BareItem.Flag(char == '1')
    }

    private fun date(): BareItem {
        position += 1
        val number = number() as? BareItem.Integer
        return number?.let { found -> BareItem.Date(found.value) } ?: failed()
    }

    private fun displayString(): BareItem {
        position += 1
        if (!consume('"')) fail()
        val out = ByteStringBuilder()
        var isOpen = !isFailed
        while (isOpen && !isFailed) {
            val char = next()
            isOpen = char != '"'
            if (isOpen) displayChar(out, char)
        }
        val text = Utf8.decodeOrNull(out.toByteString().toByteArray())
        if (text == null) fail()
        return BareItem.DisplayText(text.takeUnless { isFailed }.orEmpty())
    }

    private fun displayChar(out: ByteStringBuilder, char: Char?) {
        if (char != '%') {
            if (char == null || !StructuredGrammar.isPrintable(char)) fail()
            out.append((char ?: SPACE).code.toByte())
            return
        }
        val hex = text.substring(minOf(position, text.length), minOf(position + 2, text.length))
        val isHex = hex.length == 2 && hex.all { digit -> FieldGrammar.isDigit(digit) || digit in 'a'..'f' }
        if (!isHex) fail()
        position += 2
        out.append((hex.takeIf { isHex }?.toInt(RADIX) ?: 0).toByte())
    }

    private fun span(predicate: (Char) -> Boolean): String {
        val start = position
        while (position < text.length && predicate(text[position])) position += 1
        return text.substring(start, position)
    }

    private fun peek(): Char? = text.getOrNull(position)

    private fun next(): Char? {
        val char = peek()
        position += 1
        return char
    }

    private fun consume(char: Char): Boolean {
        if (peek() != char) return false
        position += 1
        return true
    }

    private fun skipSpaces() {
        while (peek() == SPACE) position += 1
    }

    private fun skipWhitespace() {
        while (peek()?.let(FieldGrammar::isWhitespace) == true) position += 1
    }

    private fun failed(): BareItem {
        fail()
        return placeholder.value
    }

    private fun fail() {
        isFailed = true
        position = text.length
    }
}
