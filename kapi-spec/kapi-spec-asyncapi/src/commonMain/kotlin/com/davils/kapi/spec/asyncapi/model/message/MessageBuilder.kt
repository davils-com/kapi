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

package com.davils.kapi.spec.asyncapi.model.message

import com.davils.kapi.spec.asyncapi.model.binding.MessageBindings
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [Message]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class MessageBuilder internal constructor() : ElementBuilder<Message>() {
    override val subject: String get() = "asyncapi message"

    /**
     * The schema of the headers, which describes an object.
     *
     * @since 1.0.0
     */
    public var headers: Referable<Schema>? = null

    /**
     * The schema of the payload.
     *
     * @since 1.0.0
     */
    public var payload: Referable<Schema>? = null

    /**
     * Where the correlation id of the message is found.
     *
     * @since 1.0.0
     */
    public var correlationId: Referable<CorrelationId>? = null

    /**
     * The media type of the payload, such as `application/json`; the default content type of the document applies when
     * it is `null`.
     *
     * @since 1.0.0
     */
    public var contentType: String? = null

    /**
     * A machine-friendly name of the message.
     *
     * @since 1.0.0
     */
    public var name: String? = null

    /**
     * A human-friendly title.
     *
     * @since 1.0.0
     */
    public var title: String? = null

    /**
     * A short summary of what it is about.
     *
     * @since 1.0.0
     */
    public var summary: String? = null

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    internal val tags: MutableList<Referable<Tag>> = mutableListOf()

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public var externalDocs: Referable<ExternalDocs>? = null

    /**
     * The protocol-specific details of the message.
     *
     * @since 1.0.0
     */
    public var bindings: Referable<MessageBindings>? = null

    internal val examples: MutableList<MessageExample> = mutableListOf()

    internal val traits: MutableList<Referable<MessageTrait>> = mutableListOf()

    override fun assemble(): Message = Message(this)
}
