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

package com.davils.kapi.spec.openapi.model.server

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of a [Server], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the server it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ServerBuilder internal constructor() : ElementBuilder<Server>() {
    override val subject: String get() = "openapi server"

    /**
     * The value of `url`, the address of the server; empty until it is set.
     *
     * @since 1.0.0
     */
    public var url: String = ""

    /**
     * The value of `description`, the description of the server.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    /**
     * The value of `name`, a short name of the server, written from OpenAPI 3.2 on.

     *
     * @since 1.0.0
     */
    public var name: String? = null

    internal val variables: MutableMap<String, ServerVariable> = linkedMapOf()

    override fun assemble(): Server = Server(this)
}
