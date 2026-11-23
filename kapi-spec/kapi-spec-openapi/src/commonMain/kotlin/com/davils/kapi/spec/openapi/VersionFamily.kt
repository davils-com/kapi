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

package com.davils.kapi.spec.openapi

/**
 * A line of OpenAPI versions that share one set of fields and one schema dialect.
 *
 * The entries are ordered from the oldest to the newest line; a field introduced in one line is valid in every later
 * line unless a later line removes it.
 *
 * @property prefix the major and minor version of the line, such as `3.1`.
 * @since 1.0.0
 */
public enum class VersionFamily(public val prefix: String) {
    /**
     * OpenAPI 3.0, whose schemas are an extended subset of JSON Schema draft 5 with `nullable`.
     *
     * @since 1.0.0
     */
    V30("3.0"),

    /**
     * OpenAPI 3.1, whose schemas are JSON Schema 2020-12, with webhooks, path items in components and `$ref`
     * siblings `summary` and `description`.
     *
     * @since 1.0.0
     */
    V31("3.1"),

    /**
     * OpenAPI 3.2, which adds the `QUERY` method and other methods by name, the `querystring` parameter, streamed
     * items of a media type, tag hierarchies, the `$self` of a document, the device authorization flow and media
     * types in components.
     *
     * @since 1.0.0
     */
    V32("3.2"),
    ;

    /**
     * Whether this line is [other] or a later one.
     *
     * @param other the line to compare with.
     * @return `true` if this line is [other] or newer.
     * @since 1.0.0
     */
    public fun isAtLeast(other: VersionFamily): Boolean = ordinal >= other.ordinal
}
