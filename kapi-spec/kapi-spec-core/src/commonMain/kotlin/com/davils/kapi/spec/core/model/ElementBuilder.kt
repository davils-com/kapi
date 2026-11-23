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

package com.davils.kapi.spec.core.model

import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.Extensions
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kore.core.validation.Validatable

/**
 * The builder of an [Element]: collects its fields and its extensions and validates them when the element is built.
 *
 * A builder is not safe to share between threads; the element it builds is.
 *
 * @param T the type of the element.
 * @since 1.0.0
 */
@DavilsDsl
public abstract class ElementBuilder<T : Validatable> protected constructor() :
    ValidatingBuilder<T>() {
    private val declared: MutableMap<ExtensionName, Value> = linkedMapOf()

    /**
     * Where the element was written; set by the readers of kapi.
     *
     * @since 1.0.0
     */
    @DavilsInternal
    public var location: Location? = null

    /**
     * The extensions declared so far.
     *
     * @since 1.0.0
     */
    @DavilsInternal
    public val extensions: Extensions get() = Extensions(declared)

    /**
     * Declares the extension [name] with [value]; a second declaration of the same name replaces the first.
     *
     * @param name the name of the extension.
     * @param value its value.
     * @since 1.0.0
     */
    public fun extension(name: ExtensionName, value: Value) {
        declared[name] = value
    }

    /**
     * Declares every extension of [extensions].
     *
     * @param extensions the extensions to take over.
     * @since 1.0.0
     */
    @DavilsInternal
    public fun adopt(extensions: Extensions) {
        declared.putAll(extensions.entries)
    }
}
