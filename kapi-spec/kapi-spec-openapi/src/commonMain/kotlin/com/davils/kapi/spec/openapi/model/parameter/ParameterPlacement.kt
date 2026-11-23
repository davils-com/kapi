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

package com.davils.kapi.spec.openapi.model.parameter

/**
 * Where a parameter is sent, the `in` of a parameter object.
 *
 * @property keyword the value of `in` in a document.
 * @since 1.0.0
 */
public enum class ParameterPlacement(public val keyword: String) {
    /**
     * A part of the path, named between braces in the path template, such as `petId` in `/pets/{petId}`.
     *
     * @since 1.0.0
     */
    Path("path"),

    /**
     * A pair of the query string, such as `limit` in `/pets?limit=10`.
     *
     * @since 1.0.0
     */
    Query("query"),

    /**
     * A header of the request; its name is compared without regard to case.
     *
     * @since 1.0.0
     */
    Header("header"),

    /**
     * A cookie of the request, sent in the `Cookie` header.
     *
     * @since 1.0.0
     */
    Cookie("cookie"),

    /**
     * The whole query string of a request as one parameter, declared by `querystring` and described by the one media
     * type of its content; a parameter is placed there from OpenAPI 3.2 on and has no style.

     *
     * @since 1.0.0
     */
    QueryString("querystring"),
    ;

    /**
     * Finds placements.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the placement written as [keyword], whatever the version of the document.
         *
         * @param keyword the value of `in`, which may be any text; it is compared exactly.
         * @return the placement, or `null` if [keyword] names none.
         * @since 1.0.0
         */
        public fun fromKeyword(keyword: String): ParameterPlacement? = entries.firstOrNull { it.keyword == keyword }
    }
}
