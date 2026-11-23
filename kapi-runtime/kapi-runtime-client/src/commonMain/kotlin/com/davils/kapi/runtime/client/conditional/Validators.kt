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

package com.davils.kapi.runtime.client.conditional

import com.davils.kapi.runtime.core.conditional.EntityTag
import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.response.Headers

private const val HASH_FACTOR: Int = 31

private const val ETAG: String = "etag"

private const val LAST_MODIFIED: String = "last-modified"

private const val NONE: String = "none"

/**
 * The validators of a representation by RFC 9110 §8.8: its entity tag and the time it was last modified, which a
 * later call sends back to ask whether it changed.
 *
 * Validators are immutable. A field that cannot be read, such as an entity tag with a character outside of its
 * grammar, is left out rather than guessed.
 *
 * @property entityTag the entity tag of the `ETag` field, or `null`.
 * @property lastModified the time of the `Last-Modified` field, or `null`.
 * @since 1.0.0
 */
public class Validators(public val entityTag: EntityTag? = null, public val lastModified: HttpDate? = null) {
    /**
     * Whether there is neither an entity tag nor a time, so there is nothing to revalidate.
     *
     * @since 1.0.0
     */
    public val isEmpty: Boolean get() = entityTag == null && lastModified == null

    override fun equals(other: Any?): Boolean =
        other is Validators && entityTag == other.entityTag && lastModified == other.lastModified

    override fun hashCode(): Int = entityTag.hashCode() * HASH_FACTOR + lastModified.hashCode()

    override fun toString(): String = "Validators(${entityTag ?: NONE}, ${lastModified ?: NONE})"

    /**
     * Reads validators.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the validators of an answer from its `ETag` and `Last-Modified` fields.
         *
         * @param headers the header fields of the answer, which may come from an attacker.
         * @return the validators; a field that cannot be read is left out.
         * @since 1.0.0
         */
        public fun of(headers: Headers): Validators = Validators(
            entityTag = headers[ETAG]?.let(EntityTag::parseOrNull),
            lastModified = headers[LAST_MODIFIED]?.let { text -> HttpDate.parseOrNull(text) },
        )
    }
}
