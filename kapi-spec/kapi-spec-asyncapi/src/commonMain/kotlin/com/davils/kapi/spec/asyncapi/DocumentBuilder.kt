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

package com.davils.kapi.spec.asyncapi

import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.component.Components
import com.davils.kapi.spec.asyncapi.model.info.Info
import com.davils.kapi.spec.asyncapi.model.info.InfoBuilder
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Target
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [Document]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class DocumentBuilder internal constructor() : ElementBuilder<Document>() {
    override val subject: String get() = "asyncapi document"

    /**
     * The version of AsyncAPI the document is written in.
     *
     * @since 1.0.0
     */
    public var version: Version = Version.Latest

    /**
     * The identifier of the application, an absolute URI such as `urn:example:chat`.
     *
     * @since 1.0.0
     */
    public var id: String? = null

    /**
     * The info of the application.
     *
     * @since 1.0.0
     */
    public var info: Info = InfoBuilder().build()

    internal val servers: MutableMap<String, Referable<Server>> = linkedMapOf()

    /**
     * The content type of every message that names none, such as `application/json`.
     *
     * @since 1.0.0
     */
    public var defaultContentType: String? = null

    internal val channels: MutableMap<String, Referable<Channel>> = linkedMapOf()

    internal val operations: MutableMap<String, Referable<Operation>> = linkedMapOf()

    /**
     * The reusable objects of the document.
     *
     * @since 1.0.0
     */
    public var components: Components? = null

    internal val targets: MutableMap<Target, Element> = linkedMapOf()

    override fun assemble(): Document = Document(this)
}
