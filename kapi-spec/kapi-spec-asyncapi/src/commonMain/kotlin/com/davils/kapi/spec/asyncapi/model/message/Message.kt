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
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable

/**
 * A message of a channel: its headers, its payload and what else describes it.
 *
 * [traits] holds the traits as they were declared; `Document.effective` applies them.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class Message internal constructor(builder: MessageBuilder) : Element(builder) {
    /**
     * The schema of the headers, which describes an object.
     *
     * @since 1.0.0
     */
    public val headers: Referable<Schema>? = builder.headers

    /**
     * The schema of the payload.
     *
     * @since 1.0.0
     */
    public val payload: Referable<Schema>? = builder.payload

    /**
     * Where the correlation id of the message is found.
     *
     * @since 1.0.0
     */
    public val correlationId: Referable<CorrelationId>? = builder.correlationId

    /**
     * The media type of the payload, such as `application/json`; the default content type of the document applies when
     * it is `null`.
     *
     * @since 1.0.0
     */
    public val contentType: String? = builder.contentType

    /**
     * A machine-friendly name of the message.
     *
     * @since 1.0.0
     */
    public val name: String? = builder.name

    /**
     * A human-friendly title.
     *
     * @since 1.0.0
     */
    public val title: String? = builder.title

    /**
     * A short summary of what it is about.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    /**
     * The tags, each written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val tags: List<Referable<Tag>> = builder.tags.toList()

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public val externalDocs: Referable<ExternalDocs>? = builder.externalDocs

    /**
     * The protocol-specific details of the message.
     *
     * @since 1.0.0
     */
    public val bindings: Referable<MessageBindings>? = builder.bindings

    /**
     * Examples of the message.
     *
     * @since 1.0.0
     */
    public val examples: List<MessageExample> = builder.examples.toList()

    /**
     * The traits of the message, in the order they were declared.
     *
     * @since 1.0.0
     */
    public val traits: List<Referable<MessageTrait>> = builder.traits.toList()

    override val identity: List<Any?>
        get() = listOf(
            headers,
            payload,
            correlationId,
            contentType,
            name,
            title,
            summary,
            description,
            tags,
            externalDocs,
            bindings,
            examples,
            traits,
        )
}
