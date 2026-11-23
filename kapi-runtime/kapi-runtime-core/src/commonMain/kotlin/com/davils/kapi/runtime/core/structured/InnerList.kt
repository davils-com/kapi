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

package com.davils.kapi.runtime.core.structured

private const val HASH_FACTOR: Int = 31

/**
 * An Inner List of RFC 9651 §3.1.1: items in parentheses with parameters of the list.
 *
 * An inner list is immutable; [toString] writes its serialized form.
 *
 * @param items the items in order.
 * @property parameters the parameters of the list.
 * @since 1.0.0
 */
public class InnerList(items: List<Item>, override val parameters: Parameters = Parameters.Empty) : Member {
    /**
     * The items in order.
     *
     * @since 1.0.0
     */
    public val items: List<Item> = items.toList()

    override fun equals(other: Any?): Boolean =
        other is InnerList && items == other.items && parameters == other.parameters

    override fun hashCode(): Int = items.hashCode() * HASH_FACTOR + parameters.hashCode()

    override fun toString(): String = items.joinToString(" ", prefix = "(", postfix = ")") + parameters
}
