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
 * The builder of a [License], filled by the readers of kapi.
 *
 * A builder is not safe to share between threads; the license it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class LicenseBuilder internal constructor() : ElementBuilder<License>() {
    override val subject: String get() = "openapi license"

    /**
     * The value of `name`, the name of the license; empty until it is set.
     *
     * @since 1.0.0
     */
    public var name: String = ""

    /**
     * The value of `identifier`, the SPDX license expression, written from OpenAPI 3.1 on.
     *
     * @since 1.0.0
     */
    public var identifier: String? = null

    /**
     * The value of `url`, the URL of the text of the license.
     *
     * @since 1.0.0
     */
    public var url: String? = null

    override fun assemble(): License = License(this)
}
