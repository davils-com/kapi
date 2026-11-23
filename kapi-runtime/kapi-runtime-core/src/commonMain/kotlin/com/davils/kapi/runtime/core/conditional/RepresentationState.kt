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

private const val NONE: String = "none"

/**
 * What a server knows of the selected representation of a resource when it evaluates preconditions.
 *
 * @property entityTag the current entity tag, or `null` if the server has none.
 * @property lastModified the time of the last modification, or `null` if the server has none.
 * @property isPresent whether a current representation exists, which `*` of `If-Match` and `If-None-Match` asks.
 * @since 1.0.0
 */
public class RepresentationState(
    public val entityTag: EntityTag? = null,
    public val lastModified: HttpDate? = null,
    public val isPresent: Boolean = true,
) {
    override fun equals(other: Any?): Boolean = other is RepresentationState &&
        entityTag == other.entityTag &&
        lastModified == other.lastModified &&
        isPresent == other.isPresent

    override fun hashCode(): Int = listOf(entityTag, lastModified, isPresent).hashCode()

    override fun toString(): String =
        "RepresentationState(entityTag=${entityTag ?: NONE}, lastModified=${lastModified ?: NONE}, " +
            "isPresent=$isPresent)"
}
