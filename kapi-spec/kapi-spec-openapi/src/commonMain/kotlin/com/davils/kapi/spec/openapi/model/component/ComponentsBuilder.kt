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

package com.davils.kapi.spec.openapi.model.component

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.Example
import com.davils.kapi.spec.openapi.model.content.Link
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.path.Callback
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.security.SecurityScheme
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of [Components], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the components it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ComponentsBuilder internal constructor() : ElementBuilder<Components>() {
    override val subject: String get() = "openapi components"

    internal val schemas: MutableMap<String, Schema> = KeyedMap()

    internal val responses: MutableMap<String, Referable<Response>> = KeyedMap()

    internal val parameters: MutableMap<String, Referable<Parameter>> = KeyedMap()

    internal val examples: MutableMap<String, Referable<Example>> = KeyedMap()

    internal val requestBodies: MutableMap<String, Referable<RequestBody>> = KeyedMap()

    internal val headers: MutableMap<String, Referable<Header>> = KeyedMap()

    internal val securitySchemes: MutableMap<String, Referable<SecurityScheme>> = KeyedMap()

    internal val links: MutableMap<String, Referable<Link>> = KeyedMap()

    internal val callbacks: MutableMap<String, Referable<Callback>> = KeyedMap()

    internal val pathItems: MutableMap<String, PathItem> = KeyedMap()

    internal val mediaTypes: MutableMap<String, Referable<MediaType>> = KeyedMap()

    override fun assemble(): Components = Components(this)
}
