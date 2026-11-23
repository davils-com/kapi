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

package com.davils.kapi.spec.openapi.model.path

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.content.Responses
import com.davils.kapi.spec.openapi.model.content.ResponsesBuilder
import com.davils.kapi.spec.openapi.model.info.ExternalDocs
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.security.SecurityRequirement
import com.davils.kapi.spec.openapi.model.server.Server
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of an [Operation], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the operation it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OperationBuilder internal constructor() : ElementBuilder<Operation>() {
    override val subject: String get() = "openapi operation"

    /**
     * The value of `operationId`, the id of the operation.
     *
     * @since 1.0.0
     */
    public var operationId: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `summary`, the summary of the operation.
     *
     * @since 1.0.0
     */
    public var summary: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `description`, the description of the operation.
     *
     * @since 1.0.0
     */
    public var description: String? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal val tags: MutableList<String> = mutableListOf()

    /**
     * The value of `externalDocs`, the external documentation of the operation.
     *
     * @since 1.0.0
     */
    public var externalDocs: ExternalDocs? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal val parameters: MutableList<Referable<Parameter>> = mutableListOf()

    /**
     * The value of `requestBody`, the request body of the operation.
     *
     * @since 1.0.0
     */
    public var requestBody: Referable<RequestBody>? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `responses`, the responses of the operation; without any response until it is set.
     *
     * @since 1.0.0
     */
    public var responses: Responses = ResponsesBuilder().build()
        set(value) {
            checkOpen()
            field = value
        }

    internal val callbacks: MutableMap<String, Referable<Callback>> = KeyedMap()

    /**
     * The value of `deprecated`, whether the operation should no longer be used.
     *
     * @since 1.0.0
     */
    public var isDeprecated: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The value of `security`, the security requirements of the operation; `null` to take over those of the document.
     *
     * @since 1.0.0
     */
    public var security: List<SecurityRequirement>? = null
        set(value) {
            checkOpen()
            field = value
        }

    internal val servers: MutableList<Server> = mutableListOf()

    override fun assemble(): Operation = Operation(this)
}
