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

/**
 * An HTTP method that has a field of its own in a path item, such as `get` or `post`.
 *
 * @property keyword the name of the field in a path item, in lower case, such as `get`.
 * @since 1.0.0
 */
public enum class Method(public val keyword: String) {
    /**
     * The `GET` method, declared by `get`.
     *
     * @since 1.0.0
     */
    Get("get"),

    /**
     * The `PUT` method, declared by `put`.
     *
     * @since 1.0.0
     */
    Put("put"),

    /**
     * The `POST` method, declared by `post`.
     *
     * @since 1.0.0
     */
    Post("post"),

    /**
     * The `DELETE` method, declared by `delete`.
     *
     * @since 1.0.0
     */
    Delete("delete"),

    /**
     * The `OPTIONS` method, declared by `options`.
     *
     * @since 1.0.0
     */
    Options("options"),

    /**
     * The `HEAD` method, declared by `head`.
     *
     * @since 1.0.0
     */
    Head("head"),

    /**
     * The `PATCH` method, declared by `patch`.
     *
     * @since 1.0.0
     */
    Patch("patch"),

    /**
     * The `TRACE` method, declared by `trace`.
     *
     * @since 1.0.0
     */
    Trace("trace"),

    /**
     * The `QUERY` method, declared by `query`: a safe and idempotent request that carries its query in the body; a
     * path item declares it from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    Query("query"),
    ;

    /**
     * Finds methods.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the method whose field in a path item is [keyword].
         *
         * @param keyword the name of the field, which may be any text; it is compared exactly, so `GET` names none.
         * @return the method, or `null` if [keyword] names none.
         * @since 1.0.0
         */
        public fun fromKeyword(keyword: String): Method? = entries.firstOrNull { it.keyword == keyword }
    }
}
