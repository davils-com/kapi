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

package com.davils.kapi.spec.openapi.model.security

/**
 * Where an API key is sent, the `in` of an `apiKey` security scheme.
 *
 * @property keyword the value of `in` in a document.
 * @since 1.0.0
 */
public enum class ApiKeyPlacement(public val keyword: String) {
    /**
     * A parameter of the query string.
     *
     * @since 1.0.0
     */
    Query("query"),

    /**
     * A header of the request.
     *
     * @since 1.0.0
     */
    Header("header"),

    /**
     * A cookie of the request.
     *
     * @since 1.0.0
     */
    Cookie("cookie"),
    ;

    /**
     * Finds placements of API keys.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the placement written as [keyword].
         *
         * @param keyword the value of `in`, which may be any text; it is compared exactly.
         * @return the placement, or `null` if [keyword] names none.
         * @since 1.0.0
         */
        public fun fromKeyword(keyword: String): ApiKeyPlacement? = entries.firstOrNull { it.keyword == keyword }
    }
}
