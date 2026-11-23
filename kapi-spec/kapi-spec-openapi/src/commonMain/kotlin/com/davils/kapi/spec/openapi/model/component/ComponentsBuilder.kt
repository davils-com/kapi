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

package com.davils.kapi.spec.openapi.model.component

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

    internal val schemas: MutableMap<String, Schema> = linkedMapOf()

    internal val responses: MutableMap<String, Referable<Response>> = linkedMapOf()

    internal val parameters: MutableMap<String, Referable<Parameter>> = linkedMapOf()

    internal val examples: MutableMap<String, Referable<Example>> = linkedMapOf()

    internal val requestBodies: MutableMap<String, Referable<RequestBody>> = linkedMapOf()

    internal val headers: MutableMap<String, Referable<Header>> = linkedMapOf()

    internal val securitySchemes: MutableMap<String, Referable<SecurityScheme>> = linkedMapOf()

    internal val links: MutableMap<String, Referable<Link>> = linkedMapOf()

    internal val callbacks: MutableMap<String, Referable<Callback>> = linkedMapOf()

    internal val pathItems: MutableMap<String, PathItem> = linkedMapOf()

    internal val mediaTypes: MutableMap<String, Referable<MediaType>> = linkedMapOf()

    override fun assemble(): Components = Components(this)
}
