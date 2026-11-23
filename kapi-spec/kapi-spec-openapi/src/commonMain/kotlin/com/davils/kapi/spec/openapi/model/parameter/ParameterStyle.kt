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
 * How the value of a parameter, a header or an encoded part is serialized, the `style` of OpenAPI, following the
 * forms of URI templates in RFC 6570.
 *
 * @property keyword the value of `style` in a document.
 * @since 1.0.0
 */
public enum class ParameterStyle(public val keyword: String) {
    /**
     * Path-style parameters of RFC 6570, such as `;color=blue`; for path parameters.
     *
     * @since 1.0.0
     */
    Matrix("matrix"),

    /**
     * Label expansion of RFC 6570, such as `.blue`; for path parameters.
     *
     * @since 1.0.0
     */
    Label("label"),

    /**
     * Form-style expansion of RFC 6570, such as `color=blue`; the default for query and cookie parameters.
     *
     * @since 1.0.0
     */
    Form("form"),

    /**
     * Simple expansion of RFC 6570, such as `blue,black`; the default for path parameters and headers.
     *
     * @since 1.0.0
     */
    Simple("simple"),

    /**
     * The items of an array joined by spaces, such as `blue%20black`; for query parameters.
     *
     * @since 1.0.0
     */
    SpaceDelimited("spaceDelimited"),

    /**
     * The items of an array joined by `|`, such as `blue|black`; for query parameters.
     *
     * @since 1.0.0
     */
    PipeDelimited("pipeDelimited"),

    /**
     * The properties of an object as nested names, such as `color[R]=100`; for query parameters.
     *
     * @since 1.0.0
     */
    DeepObject("deepObject"),

    /**
     * The form of the `Cookie` header, which joins the pairs of an object by `; `; a cookie parameter takes it from
     * OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    Cookie("cookie"),
    ;

    /**
     * Returns whether a parameter in [placement] may take this style.
     *
     * @param placement the place of the parameter.
     * @return `true` if OpenAPI allows the style there; always `false` for [ParameterPlacement.QueryString], which has
     * no style.
     * @since 1.0.0
     */
    public fun isAllowedIn(placement: ParameterPlacement): Boolean = this in allowed.getValue(placement)

    /**
     * Finds styles and the default of each placement.
     *
     * @since 1.0.0
     */
    public companion object {
        private val defaults: Map<ParameterPlacement, ParameterStyle> = mapOf(
            ParameterPlacement.Path to Simple,
            ParameterPlacement.Query to Form,
            ParameterPlacement.Header to Simple,
            ParameterPlacement.Cookie to Form,
        )

        private val allowed: Map<ParameterPlacement, Set<ParameterStyle>> = mapOf(
            ParameterPlacement.Path to setOf(Matrix, Label, Simple),
            ParameterPlacement.Query to setOf(Form, SpaceDelimited, PipeDelimited, DeepObject),
            ParameterPlacement.Header to setOf(Simple),
            ParameterPlacement.Cookie to setOf(Form, Cookie),
            ParameterPlacement.QueryString to emptySet(),
        )

        /**
         * The style a parameter in [placement] has when it names none.
         *
         * @param placement the place of the parameter.
         * @return the style, or `null` for the query string, which has no style.
         * @since 1.0.0
         */
        public fun defaultFor(placement: ParameterPlacement): ParameterStyle? = defaults[placement]

        /**
         * Returns the style written as [keyword], whatever the version of the document.
         *
         * @param keyword the value of `style`, which may be any text; it is compared exactly.
         * @return the style, or `null` if [keyword] names none.
         * @since 1.0.0
         */
        public fun fromKeyword(keyword: String): ParameterStyle? = entries.firstOrNull { it.keyword == keyword }
    }
}
