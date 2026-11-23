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

package com.davils.kapi.spec.openapi.model.schema

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The builder of a [Discriminator], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the discriminator it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class DiscriminatorBuilder internal constructor() :
    ElementBuilder<Discriminator>() {
    override val subject: String get() = "openapi discriminator"

    /**
     * The value of `propertyName`, the discriminating property; empty until it is set.
     *
     * @since 1.0.0
     */
    public var propertyName: String = ""

    internal val mapping: MutableMap<String, String> = KeyedMap()

    /**
     * The value of `defaultMapping`, the schema of a value that maps to no other schema, written from OpenAPI 3.2
     * on.
     *
     * @since 1.0.0
     */
    public var defaultMapping: String? = null

    override fun assemble(): Discriminator = Discriminator(this)
}
