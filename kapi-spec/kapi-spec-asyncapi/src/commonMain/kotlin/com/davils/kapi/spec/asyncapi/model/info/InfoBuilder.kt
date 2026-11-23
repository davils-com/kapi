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

package com.davils.kapi.spec.asyncapi.model.info

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of [Info]: collects its fields while a reader of kapi reads it.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class InfoBuilder internal constructor() : ElementBuilder<Info>() {
    override val subject: String get() = "asyncapi info"

    /**
     * The title of the application.
     *
     * @since 1.0.0
     */
    public var title: String = ""

    /**
     * The version of the application, not of AsyncAPI.
     *
     * @since 1.0.0
     */
    public var version: String = ""

    /**
     * A description, which may use CommonMark.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    /**
     * The address of the terms of service.
     *
     * @since 1.0.0
     */
    public var termsOfService: String? = null

    /**
     * Who to contact about the application.
     *
     * @since 1.0.0
     */
    public var contact: Contact? = null

    /**
     * The license of the application.
     *
     * @since 1.0.0
     */
    public var license: License? = null

    internal val tags: MutableList<Referable<Tag>> = mutableListOf()

    /**
     * Further documentation, written inline or as a reference.
     *
     * @since 1.0.0
     */
    public var externalDocs: Referable<ExternalDocs>? = null

    override fun assemble(): Info = Info(this)
}
