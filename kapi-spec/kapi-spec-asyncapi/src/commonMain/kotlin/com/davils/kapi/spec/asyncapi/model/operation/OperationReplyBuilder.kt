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

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [OperationReply]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OperationReplyBuilder internal constructor() : ElementBuilder<OperationReply>() {
    override val subject: String get() = "asyncapi operation reply"

    /**
     * Where the reply is sent to, given at runtime; `null` when the channel says it.
     *
     * @since 1.0.0
     */
    public var address: Referable<ReplyAddress>? = null

    /**
     * The reference to the channel of the reply.
     *
     * @since 1.0.0
     */
    public var channel: Reference? = null

    internal val messages: MutableList<Reference> = mutableListOf()

    override fun assemble(): OperationReply = OperationReply(this)
}
