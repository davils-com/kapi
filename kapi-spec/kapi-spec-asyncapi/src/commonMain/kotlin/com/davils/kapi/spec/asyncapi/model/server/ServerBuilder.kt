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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.asyncapi.model.server

import com.davils.kapi.spec.asyncapi.model.binding.ServerBindings
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of [Server]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ServerBuilder internal constructor() : ElementBuilder<Server>() {
    override val subject: String get() = "asyncapi server"

    /**
     * The host of the server, with an optional port, such as `rabbitmq.example.com:5672`.
     *
     * @since 1.0.0
     */
    public var host: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The protocol of the server, such as `ws`, `kafka` or `amqp`.
     *
     * @since 1.0.0
     */
    public var protocol: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The version of the protocol, such as `0.9.1`.
     *
     * @since 1.0.0
     */
    public var protocolVersion: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The path to a resource on the host, such as `/production`.
     *
     * @since 1.0.0
     */
    public var pathname: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public var description: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * A human-friendly title.
     *
     * @since 1.0.0
     */
    public var title: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * A short summary of what it is about.
     *
     * @since 1.0.0
     */
    public var summary: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal val variables: MutableMap<String, Referable<ServerVariable>> = KeyedMap()

    internal val security: MutableList<Referable<SecurityScheme>> = mutableListOf()

    internal val tags: MutableList<Referable<Tag>> = mutableListOf()

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public var externalDocs: Referable<ExternalDocs>? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The protocol-specific details of the server.
     *
     * @since 1.0.0
     */
    public var bindings: Referable<ServerBindings>? = null
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): Server = Server(this)
}
