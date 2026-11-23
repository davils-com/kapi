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

package com.davils.kapi.runtime.core.negotiation

private const val WILDCARD: String = "*"

private const val IDENTITY: String = "identity"

private const val SUBTAG: Char = '-'

/**
 * Chooses what a server sends among what it offers by the fields of proactive negotiation of RFC 9110 §12.
 *
 * Every choice takes the members a reader of [AcceptFields] returned, or `null` when the request carried no such
 * field, and the offers of the server in the order it prefers them. The quality of an offer is the quality of the
 * most specific member that covers it; an offer no member covers has quality zero. The offer of the highest quality
 * above zero wins, and among offers of equal quality the one the server lists first. The functions keep no state and
 * are safe to call from any thread; their work grows with the product of members and offers, which the limits of the
 * readers bound.
 *
 * @since 1.0.0
 */
public object Negotiation {
    /**
     * Returns the quality `Accept` gives [mediaType].
     *
     * @param accepted the members of `Accept`, or `null` if the request has none.
     * @param mediaType the media type to weigh.
     * @return the quality of the most specific range that includes [mediaType], [Quality.Highest] when `Accept` is
     * absent, and [Quality.NotAcceptable] when no range includes it.
     * @since 1.0.0
     */
    public fun qualityOf(accepted: List<Weighted<MediaRange>>?, mediaType: MediaType): Quality {
        if (accepted == null) return Quality.Highest
        val covering = accepted.filter { member -> member.value.includes(mediaType) }
        return covering.maxByOrNull { member -> member.value.specificity }?.quality ?: Quality.NotAcceptable
    }

    /**
     * Chooses the media type of a response.
     *
     * @param accepted the members of `Accept`, or `null` if the request has none.
     * @param offered the media types the server can send, in the order it prefers them.
     * @return the chosen media type, the first offer when `Accept` is absent, or `null` if no offer is acceptable,
     * which a server answers with 406 (Not Acceptable) or with a default of its own.
     * @since 1.0.0
     */
    public fun chooseMediaType(accepted: List<Weighted<MediaRange>>?, offered: List<MediaType>): MediaType? =
        best(offered) { mediaType -> qualityOf(accepted, mediaType) }

    /**
     * Returns the quality `Accept-Encoding` gives [coding] by RFC 9110 §12.5.3.
     *
     * @param accepted the members of `Accept-Encoding`, or `null` if the request has none.
     * @param coding the content coding, such as `gzip` or `identity`, compared without case.
     * @return the quality of [coding] if it is named, else of `*`; `identity` stays acceptable unless it or `*` is
     * weighted zero, and every coding is acceptable when the field is absent.
     * @since 1.0.0
     */
    public fun qualityOfEncoding(accepted: List<Weighted<String>>?, coding: String): Quality {
        if (accepted == null) return Quality.Highest
        val name = coding.lowercase()
        val named = accepted.firstOrNull { member -> member.value == name }
        val any = accepted.firstOrNull { member -> member.value == WILDCARD }
        val fallback = Quality.Highest.takeIf { name == IDENTITY } ?: Quality.NotAcceptable
        return named?.quality ?: any?.quality ?: fallback
    }

    /**
     * Chooses the content coding of a response.
     *
     * @param accepted the members of `Accept-Encoding`, or `null` if the request has none.
     * @param offered the codings the server can apply, such as `gzip` and `identity`, in the order it prefers them.
     * @return the chosen coding, the first offer when the field is absent, or `null` if no offer is acceptable.
     * @since 1.0.0
     */
    public fun chooseEncoding(accepted: List<Weighted<String>>?, offered: List<String>): String? =
        best(offered) { coding -> qualityOfEncoding(accepted, coding) }

    /**
     * Returns the quality `Accept-Language` gives [tag] by the basic filtering of RFC 4647 §3.3.1.
     *
     * A range covers a tag that equals it or starts with it followed by a hyphen, without case; `*` covers every tag.
     *
     * @param accepted the members of `Accept-Language`, or `null` if the request has none.
     * @param tag the language tag, such as `en-GB`.
     * @return the quality of the longest range that covers [tag], [Quality.Highest] when the field is absent, and
     * [Quality.NotAcceptable] when no range covers it.
     * @since 1.0.0
     */
    public fun qualityOfLanguage(accepted: List<Weighted<String>>?, tag: String): Quality {
        if (accepted == null) return Quality.Highest
        val lowered = tag.lowercase()
        val covering = accepted.filter { member -> covers(range = member.value, tag = lowered) }
        return covering.maxByOrNull { member -> lengthOf(member.value) }?.quality ?: Quality.NotAcceptable
    }

    /**
     * Chooses the language of a response.
     *
     * @param accepted the members of `Accept-Language`, or `null` if the request has none.
     * @param offered the language tags the server can send, in the order it prefers them.
     * @return the chosen tag as it was offered, the first offer when the field is absent, or `null` if no offer is
     * acceptable.
     * @since 1.0.0
     */
    public fun chooseLanguage(accepted: List<Weighted<String>>?, offered: List<String>): String? =
        best(offered) { tag -> qualityOfLanguage(accepted, tag) }

    private fun <T : Any> best(offered: List<T>, qualityOf: (T) -> Quality): T? {
        var chosen: T? = null
        var highest = Quality.NotAcceptable
        offered.forEach { offer ->
            val quality = qualityOf(offer)
            if (quality > highest) {
                chosen = offer
                highest = quality
            }
        }
        return chosen
    }

    private fun covers(range: String, tag: String): Boolean =
        range == WILDCARD || tag == range || tag.startsWith(range + SUBTAG)

    private fun lengthOf(range: String): Int {
        if (range == WILDCARD) return 0
        return range.length
    }
}
