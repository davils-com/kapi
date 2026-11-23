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

import com.davils.kapi.spec.core.model.Element

/**
 * The `license` object of an info: the license the API is offered under.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class License internal constructor(builder: LicenseBuilder) : Element(builder) {
    /**
     * The `name` of the license, such as `Apache 2.0`; required by OpenAPI, so a reading reports a license that leaves
     * it out and keeps it empty here.
     *
     * @since 1.0.0
     */
    public val name: String = builder.name

    /**
     * The SPDX license expression of the license in `identifier`, such as `Apache-2.0`; `null` when the document leaves
     * it out and before OpenAPI 3.1. OpenAPI allows it only instead of [url].
     *
     * @since 1.0.0
     */
    public val identifier: String? = builder.identifier

    /**
     * The `url` of the text of the license, as it was written and not checked; `null` when the document leaves it out.
     *
     * @since 1.0.0
     */
    public val url: String? = builder.url

    override val identity: List<Any?> get() = listOf(name, identifier, url)
}
