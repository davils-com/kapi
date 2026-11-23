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
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A `$ref` as it was written, with the place it leads to.
 *
 * @property text the reference as it was written, such as `#/components/schemas/Pet`.
 * @property target the document and the JSON Pointer the reference leads to.
 * @property summary the `summary` written beside the reference, which overrides the one of the target where the
 * specification allows it.
 * @property description the `description` written beside the reference, likewise.
 * @property location where the reference was written, or `null` for one built in code.
 * @since 1.0.0
 */
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

    override fun toString(): String = "Reference(${SafeText.quoted(text)} -> $target)"
}
