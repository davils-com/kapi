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

package com.davils.kapi.spec.asyncapi.model.operation

import com.davils.kapi.spec.asyncapi.model.binding.OperationBindings
import com.davils.kapi.spec.asyncapi.model.info.ExternalDocs
import com.davils.kapi.spec.asyncapi.model.info.Tag
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [Operation]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OperationBuilder internal constructor() : ElementBuilder<Operation>() {
    override val subject: String get() = "asyncapi operation"

    /**
     * Whether the application sends or receives the messages.
     *
     * @since 1.0.0
     */
    public var action: OperationAction = OperationAction.Send

    /**
     * The reference to the channel of the operation; `null` when the document misses it.
     *
     * @since 1.0.0
     */
    public var channel: Reference? = null

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

    internal val security: MutableList<Referable<SecurityScheme>> = mutableListOf()

    internal val tags: MutableList<Referable<Tag>> = mutableListOf()

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public var externalDocs: Referable<ExternalDocs>? = null

    /**
     * The protocol-specific details of the operation.
     *
     * @since 1.0.0
     */
    public var bindings: Referable<OperationBindings>? = null

    internal val traits: MutableList<Referable<OperationTrait>> = mutableListOf()

    internal val messages: MutableList<Reference> = mutableListOf()

    /**
     * The reply to the operation, for request and reply.
     *
     * @since 1.0.0
     */
    public var reply: Referable<OperationReply>? = null

    override fun assemble(): Operation = Operation(this)
}
