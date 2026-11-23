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

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.FieldLists

private const val ANY: String = "*"

/**
 * The value of `If-Match` or `If-None-Match` of RFC 9110 §13.1.1 and §13.1.2: `*` or a list of entity tags.
 *
 * @since 1.0.0
 */
public sealed interface EntityTagCondition {
    /**
     * `*`: any current representation.
     *
     * @since 1.0.0
     */
    public object Any : EntityTagCondition {
        override fun toString(): String = ANY
    }

    /**
     * A list of entity tags.
     *
     * @param tags the tags in order.
     * @since 1.0.0
     */
    public class Tags(tags: List<EntityTag>) : EntityTagCondition {
        /**
         * The tags in order.
         *
         * @since 1.0.0
         */
        public val entityTags: List<EntityTag> = tags.toList()

        override fun equals(other: kotlin.Any?): Boolean = other is Tags && entityTags == other.entityTags

        override fun hashCode(): Int = entityTags.hashCode()

        override fun toString(): String = entityTags.joinToString(", ")
    }

    /**
     * Reads the conditions.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the value of `If-Match` or `If-None-Match`.
         *
         * @param text the field value, the lines of a repeated field joined with commas.
         * @param limits the limits of the field: its length and the number of its tags.
         * @return the condition, or `null` if the value is malformed or exceeds a limit. [Preconditions] reads a
         * malformed list as one that matches nothing, which fails `If-Match` and passes `If-None-Match`.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String, limits: FieldLimits = FieldLimits.Default): EntityTagCondition? {
            if (FieldGrammar.trimmed(text) == ANY) return Any
            val tags = FieldLists.read(
                text = text,
                maxLength = limits.maxFieldLength,
                maxMembers = limits.maxMembers,
                element = EntityTag::read,
            )
            return tags?.let(::Tags)
        }
    }
}
