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

package com.davils.kapi.spec.openapi.model.content

import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.parameter.Header

public class Response internal constructor(builder: ResponseBuilder) :
    Element(builder) {
    /**
     * A short summary of the response; `null` before OpenAPI 3.2.
     *
     * @since 1.0.0
     */
    public val summary: String? = builder.summary

    /**
     * The description of the response; `null` when the document leaves it out, which OpenAPI allows from 3.2 on and
     * a reading of an older document reports.
     *
     * @since 1.0.0
     */
    public val description: String? = builder.description

    public val headers: Map<String, Referable<Header>> = builder.headers.toMap()

    public val content: Map<String, Referable<MediaType>> = builder.content.toMap()

    public val links: Map<String, Referable<Link>> = builder.links.toMap()

    override val identity: List<Any?> get() = listOf(summary, description, headers, content, links)
}
