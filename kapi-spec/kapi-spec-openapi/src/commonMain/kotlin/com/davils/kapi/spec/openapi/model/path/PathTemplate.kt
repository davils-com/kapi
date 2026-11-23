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

package com.davils.kapi.spec.openapi.model.path

import kotlin.jvm.JvmInline

private const val ROOT: String = "/"

/**
 * A key of the `paths` object: the path of an endpoint relative to the address of the server, such as
 * `/pets/{petId}`, in which every name between braces is a path parameter.
 *
 * Only the leading slash is checked when a template is created; a reading reports a template that names a parameter
 * twice or has a stray brace, and templates that differ only by the names of their parameters.
 *
 * @property value the template as it was written.
 * @throws IllegalArgumentException if [value] does not start with `/`.
 * @since 1.0.0
 */
@JvmInline
public value class PathTemplate(public val value: String) : Comparable<PathTemplate> {
    init {
        require(value.startsWith(ROOT)) { "'$value' is no path template such as /pets/{petId}" }
    }

    /**
     * The names of the path parameters between braces, in the order they appear; empty for a template without
     * parameters.
     *
     * @since 1.0.0
     */
    public val parameters: List<String>
        get() = Placeholders.names(text = value, stops = Placeholders.PATH_STOPS)

    override fun toString(): String = value

    /**
     * Compares the templates by their text, so that they can key an ordered map.
     *
     * @param other the other one.
     * @return a negative number, zero or a positive number as this one sorts before, with or after [other].
     * @since 1.0.0
     */
    override fun compareTo(other: PathTemplate): Int = value.compareTo(other.value)

    /**
     * Creates path templates from text.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns [text] as a path template, or `null` instead of throwing.
         *
         * @param text the key of the `paths` object as it was written.
         * @return the template, or `null` if [text] does not start with `/`.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): PathTemplate? {
            if (!text.startsWith(ROOT)) return null
            return PathTemplate(text)
        }
    }
}
