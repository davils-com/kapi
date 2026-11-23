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

public abstract class Element protected constructor(builder: ElementBuilder<*>) : Validatable {
    public val location: Location? = builder.location

    public val extensions: Extensions = builder.extensions

    protected abstract val identity: List<Any?>

    internal val children: List<Element> get() = ElementWalk.childrenOf(identity)

    public fun walk(): Sequence<Element> = ElementWalk.from(this)

    override fun ValidationScope.validate(): Unit = Unit

    final override fun equals(other: Any?): Boolean =
        other is Element && other::class == this::class && sameContent(other)

    final override fun hashCode(): Int = identity.hashCode()

    final override fun toString(): String {
        val place = location ?: BUILT
        return "${this::class.simpleName}($place)"
    }

    private fun sameContent(other: Element): Boolean = identity == other.identity && extensions == other.extensions
}
