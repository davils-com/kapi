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

package com.davils.kapi.spec.openapi.model.schema

/**
 * A type of JSON value, as the `type` of a schema names it.
 *
 * @property keyword the name of the type in a document.
 * @since 1.0.0
 */
public enum class SchemaType(public val keyword: kotlin.String) {
    /**
     * The JSON `null`. In OpenAPI 3.0 a schema allows it by `nullable`, which a reading turns into this type.
     *
     * @since 1.0.0
     */
    Null("null"),

    /**
     * `true` or `false`.
     *
     * @since 1.0.0
     */
    Boolean("boolean"),

    /**
     * A number without a fraction; JSON Schema 2020-12 counts `1.0` among them.
     *
     * @since 1.0.0
     */
    Integer("integer"),

    /**
     * Any number, integers included.
     *
     * @since 1.0.0
     */
    Number("number"),

    /**
     * A string.
     *
     * @since 1.0.0
     */
    String("string"),

    /**
     * An array.
     *
     * @since 1.0.0
     */
    Array("array"),

    /**
     * An object.
     *
     * @since 1.0.0
     */
    Object("object"),
    ;

    /**
     * Finds types.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the type named [keyword].
         *
         * @param keyword the name of the type, which may be any text; it is compared exactly.
         * @return the type, or `null` if [keyword] names none.
         * @since 1.0.0
         */
        public fun fromKeyword(keyword: kotlin.String): SchemaType? = entries.firstOrNull { it.keyword == keyword }
    }
}
