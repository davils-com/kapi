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

public enum class Method(public val keyword: String) {
    Get("get"),
    Put("put"),
    Post("post"),
    Delete("delete"),
    Options("options"),
    Head("head"),
    Patch("patch"),
    Trace("trace"),

    /**
     * The `QUERY` method, a safe and idempotent request that carries its query in the body; a path item declares it
     * from OpenAPI 3.2 on.
     *
     * @since 1.0.0
     */
    Query("query"),
    ;

    public companion object {
        public fun fromKeyword(keyword: String): Method? = entries.firstOrNull { it.keyword == keyword }
    }
}
