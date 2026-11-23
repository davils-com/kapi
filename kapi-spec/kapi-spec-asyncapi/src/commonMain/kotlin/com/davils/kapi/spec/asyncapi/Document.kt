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
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Target

/**
 * An AsyncAPI document: the application it describes, its servers, channels and operations, and the components they
 * share.
 *
 * [targets] holds every object a reference of the document leads to, in this document or another one, by the place it
 * was read from; `resolve` finds the object of a reference.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public class Document internal constructor(builder: DocumentBuilder) : Element(builder) {
    /**
     * The version of AsyncAPI the document is written in.
     *
     * @since 1.0.0
     */
    public val version: Version = builder.version

    /**
     * The identifier of the application, an absolute URI such as `urn:example:chat`.
     *
     * @since 1.0.0
     */
    public val id: String? = builder.id

    /**
     * The info of the application.
     *
     * @since 1.0.0
     */
    public val info: Info = builder.info

    /**
     * The servers of the application, by their key.
     *
     * @since 1.0.0
     */
    public val servers: Map<String, Referable<Server>> = builder.servers.toMap()

    /**
     * The content type of every message that names none, such as `application/json`.
     *
     * @since 1.0.0
     */
    public val defaultContentType: String? = builder.defaultContentType

    /**
     * The channels of the application, by their id.
     *
     * @since 1.0.0
     */
    public val channels: Map<String, Referable<Channel>> = builder.channels.toMap()

    /**
     * The operations of the application, by their id.
     *
     * @since 1.0.0
     */
    public val operations: Map<String, Referable<Operation>> = builder.operations.toMap()

    /**
     * The reusable objects of the document.
     *
     * @since 1.0.0
     */
    public val components: Components? = builder.components

    /**
     * The objects the references of the document lead to, by the place they were read from.
     *
     * @since 1.0.0
     */
    public val targets: Map<Target, Element> = builder.targets.toMap()

    override val identity: List<Any?>
        get() = listOf(
            version,
            id,
            info,
            servers,
            defaultContentType,
            channels,
            operations,
            components,
            targets,
        )
}
