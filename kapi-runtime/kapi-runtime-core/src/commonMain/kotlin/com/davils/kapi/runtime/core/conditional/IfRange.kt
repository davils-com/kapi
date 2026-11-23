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

package com.davils.kapi.runtime.core.conditional

import com.davils.kapi.runtime.core.internal.FieldGrammar

/**
 * The value of `If-Range` of RFC 9110 §13.1.5: an entity tag or an HTTP-date the client last saw.
 *
 * @since 1.0.0
 */
public sealed interface IfRange {
    /**
     * Returns whether the range of the request may be served from [state] by RFC 9110 §13.1.5.
     *
     * @param state the current representation.
     * @return `true` if the entity tag matches the current one strongly or the date equals the last modification
     * exactly; a weak entity tag never matches.
     * @since 1.0.0
     */
    public fun matches(state: RepresentationState): Boolean

    /**
     * An entity tag.
     *
     * @property tag the entity tag.
     * @since 1.0.0
     */
    public class Tag(public val tag: EntityTag) : IfRange {
        override fun matches(state: RepresentationState): Boolean {
            val current = state.entityTag ?: return false
            return tag.matchesStrongly(current)
        }

        override fun equals(other: Any?): Boolean = other is Tag && tag == other.tag

        override fun hashCode(): Int = tag.hashCode()

        override fun toString(): String = tag.toString()
    }

    /**
     * An HTTP-date.
     *
     * @property date the date.
     * @since 1.0.0
     */
    public class Date(public val date: HttpDate) : IfRange {
        override fun matches(state: RepresentationState): Boolean = state.lastModified == date

        override fun equals(other: Any?): Boolean = other is Date && date == other.date

        override fun hashCode(): Int = date.hashCode()

        override fun toString(): String = date.toString()
    }

    /**
     * Reads `If-Range`.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the value of `If-Range`.
         *
         * @param text the field value, which may come from an attacker.
         * @param now the present, to read an HTTP-date with a two-digit year.
         * @return the condition, or `null` if [text] is neither an entity tag nor an HTTP-date, in which case the
         * range must not be served.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String, now: HttpDate? = null): IfRange? {
            val tag = EntityTag.parseOrNull(text)?.let(::Tag)
            return tag ?: HttpDate.parseOrNull(FieldGrammar.trimmed(text), now)?.let(::Date)
        }
    }
}
