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

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference

/**
 * The reply to an operation: where it is sent to and which messages it may be.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class OperationReply internal constructor(builder: OperationReplyBuilder) : Element(builder) {
    /**
     * Where the reply is sent to, given at runtime; `null` when the channel says it.
     *
     * @since 1.0.0
     */
    public val address: Referable<ReplyAddress>? = builder.address

    /**
     * The reference to the channel of the reply.
     *
     * @since 1.0.0
     */
    public val channel: Reference? = builder.channel

    /**
     * The references to the messages of the reply channel the reply may be.
     *
     * @since 1.0.0
     */
    public val messages: List<Reference> = builder.messages.toList()

    override val identity: List<Any?> get() = listOf(address, channel, messages)
}
