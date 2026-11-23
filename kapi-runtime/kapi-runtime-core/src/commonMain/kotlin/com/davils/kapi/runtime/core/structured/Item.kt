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
 * An Item of RFC 9651 §3.3: a bare item with parameters.
 *
 * An item is immutable; [toString] writes its serialized form.
 *
 * @property value the bare item.
 * @property parameters the parameters.
 * @since 1.0.0
 */
public class Item(public val value: BareItem, override val parameters: Parameters = Parameters.Empty) : Member {
    override fun equals(other: Any?): Boolean = other is Item && value == other.value && parameters == other.parameters

    override fun hashCode(): Int = value.hashCode() * HASH_FACTOR + parameters.hashCode()

    override fun toString(): String = "$value$parameters"
}
