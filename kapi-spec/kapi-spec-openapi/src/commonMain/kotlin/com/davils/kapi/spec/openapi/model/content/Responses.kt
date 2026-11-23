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

/**
 * The `responses` object of an operation: its expected responses by status.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Responses internal constructor(builder: ResponsesBuilder) :
    Element(builder) {
    /**
     * The responses by status, inline or by reference, in the order of the document; empty when the operation declares
     * none, which a reading reports.
     *
     * @since 1.0.0
     */
    public val entries: Map<ResponseStatus, Referable<Response>> =
        builder.entries.toMap()

    override val identity: List<Any?> get() = listOf(entries)
}
