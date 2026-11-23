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

package com.davils.kapi.runtime.server.request

/**
 * Where a parameter of OpenAPI 3.2 §4.12 lies in a request.
 *
 * @property label the name of the place in the paths of violations, such as `query`.
 * @since 1.0.0
 */
public enum class Place(public val label: String) {
    /**
     * A segment of the path.
     *
     * @since 1.0.0
     */
    Path("path"),

    /**
     * The query.
     *
     * @since 1.0.0
     */
    Query("query"),

    /**
     * A header field.
     *
     * @since 1.0.0
     */
    Header("header"),

    /**
     * A cookie of the `Cookie` field.
     *
     * @since 1.0.0
     */
    Cookie("cookie"),
}
