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

package com.davils.kapi.spec.openapi.model.info

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kore.core.annotation.DavilsDsl

/**
 * The builder of an [Info], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the info it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class InfoBuilder internal constructor() : ElementBuilder<Info>() {
    override val subject: String get() = "openapi info"

    /**
     * The value of `title`, the title of the API; empty until it is set.
     *
     * @since 1.0.0
     */
    public var title: String = ""

    /**
     * The value of `version`, the version of the document; empty until it is set.
     *
     * @since 1.0.0
     */
    public var version: String = ""

    /**
     * The value of `summary`, a short summary of the API, written from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public var summary: String? = null

    /**
     * The value of `description`, the description of the API.
     *
     * @since 1.0.0
     */
    public var description: String? = null

    /**
     * The value of `termsOfService`, the URL of the terms of service of the API.
     *
     * @since 1.0.0
     */
    public var termsOfService: String? = null

    /**
     * The value of `contact`, the contact of the API.
     *
     * @since 1.0.0
     */
    public var contact: Contact? = null

    /**
     * The value of `license`, the license of the API.
     *
     * @since 1.0.0
     */
    public var license: License? = null

    override fun assemble(): Info = Info(this)
}
