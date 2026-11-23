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

public class Reference(
    public val text: String,
    public val target: Target,
    public val summary: String? = null,
    public val description: String? = null,
    public val location: Location? = null,
) {
    private val identity: List<Any?> get() = listOf(text, target, summary, description)

    override fun equals(other: Any?): Boolean = other is Reference && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String = "Reference($text -> $target)"
}
