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

package com.davils.kapi.spec.asyncapi.model.security

/**
 * Where the key of an [HttpApiKeyScheme] is sent.
 *
 * @property keyword the value of the field `in`.
 * @since 1.0.0
 */
public enum class HttpApiKeyPlacement(public val keyword: String) {
    /**
     * In a query parameter.
     *
     * @since 1.0.0
     */
    Query("query"),

    /**
     * In a header.
     *
     * @since 1.0.0
     */
    Header("header"),

    /**
     * In a cookie.
     *
     * @since 1.0.0
     */
    Cookie("cookie"),
}
