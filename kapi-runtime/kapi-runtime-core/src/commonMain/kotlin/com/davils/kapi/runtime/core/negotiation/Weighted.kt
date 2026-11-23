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

package com.davils.kapi.runtime.core.negotiation

private const val HASH_FACTOR: Int = 31

/**
 * A member of a field of proactive negotiation, such as `text/html;q=0.8` of `Accept`, with the quality the client
 * gives it.
 *
 * It is immutable when [value] is, which every value the runtime reads is.
 *
 * @param T the type of the member.
 * @property value the media range, the content coding or the language range.
 * @property quality the quality; [Quality.Highest] when the member carries no weight.
 * @since 1.0.0
 */
public class Weighted<out T : Any>(public val value: T, public val quality: Quality = Quality.Highest) {
    override fun equals(other: Any?): Boolean = other is Weighted<*> && value == other.value && quality == other.quality

    override fun hashCode(): Int = value.hashCode() * HASH_FACTOR + quality.hashCode()

    override fun toString(): String = "$value;q=$quality"
}
