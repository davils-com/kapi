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

package com.davils.kapi.spec.openapi.model.path

import com.davils.kapi.spec.core.model.Element

/**
 * The `paths` object of a document: the endpoints of the API and the operations they offer.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Paths internal constructor(builder: PathsBuilder) : Element(builder) {
    /**
     * The path items by their path template, in the order of the document; empty when the document declares none.
     *
     * A reading leaves out a key that does not start with `/` and reports it.
     *
     * @since 1.0.0
     */
    public val items: Map<PathTemplate, PathItem> = builder.items.toMap()

    override val identity: List<Any?> get() = listOf(items)
}
