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
 * A callback object: the requests the API may send on its own to an address the caller gave, related to an
 * operation.
 *
 * The element is immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Callback internal constructor(builder: CallbackBuilder) :
    Element(builder) {
    /**
     * The path items by the runtime expression that yields the address of the callback, such as
     * `{$request.body#/callbackUrl}`, in the order of the document; empty when the callback declares none.
     *
     * The expressions are kept as they were written and not evaluated.
     *
     * @since 1.0.0
     */
    public val expressions: Map<String, PathItem> = builder.expressions.toMap()

    override val identity: List<Any?> get() = listOf(expressions)
}
