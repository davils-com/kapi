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

public enum class ParameterStyle(public val keyword: String) {
    Matrix("matrix"),
    Label("label"),
    Form("form"),
    Simple("simple"),
    SpaceDelimited("spaceDelimited"),
    PipeDelimited("pipeDelimited"),
    DeepObject("deepObject"),

    /**
     * The form of the `Cookie` header, which joins the pairs of an object by `; `; a cookie parameter takes it from
     * OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    Cookie("cookie"),
    ;

    public fun isAllowedIn(placement: ParameterPlacement): Boolean = this in allowed.getValue(placement)

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

        public fun fromKeyword(keyword: String): ParameterStyle? = entries.firstOrNull { it.keyword == keyword }
    }
}
