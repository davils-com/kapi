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

package com.davils.kapi.spec.core.model

import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.value.Extensions
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val BUILT: String = "built"

private const val ELEMENT: String = "Element"

/**
 * An object of an API document, such as an operation, a schema or a server, built by its [ElementBuilder].
 *
 * Elements are immutable and safe to share between threads. Two elements are equal when they are of the same class
 * and hold the same content and extensions; where they were written does not count. [toString] names only the class
 * and the location, never content of the document.
 *
 * @param builder the builder whose state the element takes over.
 * @since 1.0.0
 */
public abstract class Element protected constructor(builder: ElementBuilder<*>) : Validatable {
    /**
     * Where the element was written, or `null` for an element built in code.
     *
     * @since 1.0.0
     */
    public val location: Location? = builder.location

    /**
     * The specification extensions of the element.
     *
     * @since 1.0.0
     */
    public val extensions: Extensions = builder.extensions

    /**
     * The content of the element that [equals] and [hashCode] compare, in a fixed order.
     *
     * @since 1.0.0
     */
    protected abstract val identity: List<Any?>

    internal val children: List<Element> get() = ElementWalk.childrenOf(identity)

    /**
     * Returns this element and every element it holds, inline or in collections, depth first in the order of their
     * fields; references are not followed.
     *
     * @return the elements, starting with this one.
     * @since 1.0.0
     */
    public fun walk(): Sequence<Element> = ElementWalk.from(this)

    override fun ValidationScope.validate(): Unit = Unit

    final override fun equals(other: Any?): Boolean =
        other is Element && other::class == this::class && sameContent(other)

    final override fun hashCode(): Int = identity.hashCode()

    final override fun toString(): String {
        val place = location ?: BUILT
        val name = this::class.simpleName ?: ELEMENT
        return "$name($place)"
    }

    private fun sameContent(other: Element): Boolean = identity == other.identity && extensions == other.extensions
}
