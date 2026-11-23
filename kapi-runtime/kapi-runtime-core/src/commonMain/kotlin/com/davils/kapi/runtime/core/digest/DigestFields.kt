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

package com.davils.kapi.runtime.core.digest

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.Dictionary
import com.davils.kapi.runtime.core.structured.Item
import com.davils.kapi.runtime.core.structured.StructuredFields
import kotlinx.io.bytestring.ByteString

private const val MOST_WEIGHT: Long = 10L

/**
 * Reads and writes the integrity fields of RFC 9530: `Content-Digest` and `Repr-Digest`, Dictionaries of algorithms
 * with Byte Sequences, and `Want-Content-Digest` and `Want-Repr-Digest`, Dictionaries of algorithms with weights.
 *
 * The functions handle the syntax only: computing a digest and comparing it in constant time is the caller's work.
 * A value that is no Dictionary of RFC 9651 or exceeds the [FieldLimits] is refused as a whole; a member of the wrong
 * type, such as a digest that is no Byte Sequence or a weight outside 0 to 10, is ignored. Keys stay as written, so
 * algorithms outside the registry of [DigestAlgorithm] are kept. The functions keep no state and are safe to call from
 * any thread.
 *
 * @since 1.0.0
 */
public object DigestFields {
    /**
     * Reads the value of `Content-Digest` or `Repr-Digest`.
     *
     * @param text the field value, the lines of a repeated field joined with commas.
     * @param limits the limits of the Dictionary.
     * @return the digests in order, or `null` if [text] is no Dictionary or exceeds a limit.
     * @since 1.0.0
     */
    public fun parseDigests(text: String, limits: FieldLimits = FieldLimits.Default): List<Pair<String, ByteString>>? {
        val dictionary = StructuredFields.parseDictionaryOrNull(text, limits) ?: return null
        return dictionary.entries.mapNotNull { (key, member) ->
            val bytes = ((member as? Item)?.value as? BareItem.Bytes)?.value
            bytes?.let { value -> key to value }
        }
    }

    /**
     * Writes [digests] as the value of `Content-Digest` or `Repr-Digest`.
     *
     * @param digests the algorithm keys, such as `sha-256`, with the digests, in order.
     * @return the field value, such as `sha-256=:X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=:`.
     * @throws IllegalArgumentException if a key is no key of RFC 9651 or is named twice.
     * @since 1.0.0
     */
    public fun formatDigests(digests: List<Pair<String, ByteString>>): String =
        Dictionary(digests.map { (key, value) -> key to Item(BareItem.Bytes(value)) }).toString()

    /**
     * Reads the value of `Want-Content-Digest` or `Want-Repr-Digest`.
     *
     * @param text the field value, the lines of a repeated field joined with commas.
     * @param limits the limits of the Dictionary.
     * @return the algorithm keys with their weights from 0, not acceptable, to 10, most preferred, in order, or `null`
     * if [text] is no Dictionary or exceeds a limit.
     * @since 1.0.0
     */
    public fun parsePreferences(text: String, limits: FieldLimits = FieldLimits.Default): List<Pair<String, Int>>? {
        val dictionary = StructuredFields.parseDictionaryOrNull(text, limits) ?: return null
        return dictionary.entries.mapNotNull { (key, member) ->
            val weight = ((member as? Item)?.value as? BareItem.Integer)?.value?.takeIf { value -> value in 0..MOST_WEIGHT }
            weight?.let { value -> key to value.toInt() }
        }
    }

    /**
     * Writes [preferences] as the value of `Want-Content-Digest` or `Want-Repr-Digest`.
     *
     * @param preferences the algorithm keys with their weights from 0 to 10, in order.
     * @return the field value, such as `sha-512=3, sha-256=10`.
     * @throws IllegalArgumentException if a key is no key of RFC 9651 or named twice, or a weight lies outside 0 to 10.
     * @since 1.0.0
     */
    public fun formatPreferences(preferences: List<Pair<String, Int>>): String {
        require(preferences.all { (_, weight) -> weight in 0..MOST_WEIGHT }) { "A weight lies between 0 and 10" }
        return Dictionary(preferences.map { (key, weight) -> key to Item(BareItem.Integer(weight.toLong())) }).toString()
    }

    /**
     * Chooses the digest to check among [digests]: the first of the strongest active algorithm.
     *
     * @param digests the digests of a field.
     * @return the algorithm with its digest, or `null` if no digest uses an active algorithm.
     * @since 1.0.0
     */
    public fun strongest(digests: List<Pair<String, ByteString>>): Pair<DigestAlgorithm, ByteString>? {
        val known = digests.mapNotNull { (key, value) -> DigestAlgorithm.of(key)?.let { algorithm -> algorithm to value } }
        return known.filter { (algorithm, _) -> algorithm.isActive }.minByOrNull { (algorithm, _) -> algorithm.ordinal }
    }

    /**
     * Chooses the algorithm to send a digest with, by the weights of a want field.
     *
     * @param preferences the keys with their weights of a want field.
     * @param supported the algorithms the sender can compute, in the order it prefers them.
     * @return the supported algorithm of the highest weight above zero, the first supported one of equal weights, or
     * `null` if none is wanted.
     * @since 1.0.0
     */
    public fun choose(preferences: List<Pair<String, Int>>, supported: List<DigestAlgorithm>): DigestAlgorithm? {
        val weighted = supported.map { algorithm ->
            algorithm to (preferences.firstOrNull { (key, _) -> DigestAlgorithm.of(key) == algorithm }?.second ?: 0)
        }
        val best = weighted.maxOfOrNull { (_, weight) -> weight } ?: 0
        return weighted.firstOrNull { (_, weight) -> weight == best && weight > 0 }?.first
    }
}
