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

package com.davils.kapi.spec.asyncapi.model.binding

/**
 * The HTTP method of the request that opens a WebSocket connection.
 *
 * @property keyword the value of the field `method`.
 * @since 1.0.0
 */
public enum class WebSocketMethod(public val keyword: String) {
    /**
     * `GET`, the method of the opening handshake.
     *
     * @since 1.0.0
     */
    Get("GET"),

    /**
     * `POST`.
     *
     * @since 1.0.0
     */
    Post("POST"),
}
