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

package com.davils.kapi.spec.asyncapi.model.channel

import com.davils.kapi.spec.asyncapi.model.binding.ChannelBindings
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of [Channel]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ChannelBuilder internal constructor() : ElementBuilder<Channel>() {
    override val subject: String get() = "asyncapi channel"

    /**
     * The address of the channel, such as a topic, a routing key or a path; `null` when it is dynamic or unknown.
     *
     * @since 1.0.0
     */
    public var address: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal val messages: MutableMap<String, Referable<Message>> = KeyedMap()

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

    internal val servers: MutableList<Reference> = mutableListOf()

    internal val parameters: MutableMap<String, Referable<Parameter>> = KeyedMap()

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
     * The protocol-specific details of the channel.
     *
     * @since 1.0.0
     */
    public var bindings: Referable<ChannelBindings>? = null
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): Channel = Channel(this)
}
