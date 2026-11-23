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

package com.davils.kapi.runtime.core.structured

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.structured.internal.StructuredParser

/**
 * Reads and writes the Structured Field Values of RFC 9651: Items, Lists and Dictionaries with their parameters.
 *
 * The readers follow the parsing algorithms of RFC 9651 §4.2 strictly: a field value that is not all ASCII, that is
 * malformed anywhere, or that exceeds the [FieldLimits] it is handed is refused as a whole, as §4.2 demands, and a
 * caller then ignores the field. [FieldLimits.maxMembers] bounds the members of a List or a Dictionary and the items
 * of every Inner List, [FieldLimits.maxParameters] the parameters of every member. A key named twice keeps the last
 * value at the place of the first. The writers follow §4.1 and cannot fail, because every type checks its values when
 * it is made. The functions keep no state and are safe to call from any thread.
 *
 * @since 1.0.0
 */
public object StructuredFields {
    /**
     * Reads [text] as an Item.
     *
     * @param text the field value, the lines of a repeated field joined with commas.
     * @param limits the limits to keep.
     * @return the item, or `null` if [text] is no item or exceeds a limit.
     * @since 1.0.0
     */
    public fun parseItemOrNull(text: String, limits: FieldLimits = FieldLimits.Default): Item? =
        StructuredParser(text, limits).parseItem()

    /**
     * Reads [text] as a List.
     *
     * @param text the field value, the lines of a repeated field joined with commas.
     * @param limits the limits to keep.
     * @return the members in order, an empty list for an empty value, or `null` if [text] is no list or exceeds a
     * limit.
     * @since 1.0.0
     */
    public fun parseListOrNull(text: String, limits: FieldLimits = FieldLimits.Default): List<Member>? =
        StructuredParser(text, limits).parseList()

    /**
     * Reads [text] as a Dictionary.
     *
     * @param text the field value, the lines of a repeated field joined with commas.
     * @param limits the limits to keep.
     * @return the dictionary, empty for an empty value, or `null` if [text] is no dictionary or exceeds a limit.
     * @since 1.0.0
     */
    public fun parseDictionaryOrNull(text: String, limits: FieldLimits = FieldLimits.Default): Dictionary? =
        StructuredParser(text, limits).parseDictionary()

    /**
     * Writes [item] as a field value.
     *
     * @param item the item.
     * @return the serialized item.
     * @since 1.0.0
     */
    public fun serialize(item: Item): String = item.toString()

    /**
     * Writes [members] as the field value of a List.
     *
     * @param members the members in order.
     * @return the serialized list, empty for no members, in which case the field should not be sent.
     * @since 1.0.0
     */
    public fun serializeList(members: List<Member>): String = members.joinToString(", ")

    /**
     * Writes [dictionary] as a field value.
     *
     * @param dictionary the dictionary.
     * @return the serialized dictionary, empty for no entries, in which case the field should not be sent.
     * @since 1.0.0
     */
    public fun serialize(dictionary: Dictionary): String = dictionary.toString()
}
