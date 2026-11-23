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

package com.davils.kapi.runtime.core.problem

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.Serializable

private const val HASH_FACTOR: Int = 31

/**
 * One violation of an invalid request, the member `violations` of a [Problem] carries a list of.
 *
 * Every member is required when a violation is read. [toString] shows the members escaped.
 *
 * @property path where the violation lies, such as `pets[0].name`.
 * @property code the stable code of the violation, such as `kapi.runtime.constraint.min-length`.
 * @property message what is wrong, for a person to read.
 * @since 1.0.0
 */
@Serializable
public class ProblemViolation(
    public val path: String,
    public val code: String,
    public val message: String,
) {
    override fun equals(other: Any?): Boolean =
        other is ProblemViolation && path == other.path && code == other.code && message == other.message

    override fun hashCode(): Int = (path.hashCode() * HASH_FACTOR + code.hashCode()) * HASH_FACTOR + message.hashCode()

    override fun toString(): String = "ProblemViolation(path=${SafeText.text(path)}, code=${SafeText.text(code)}, " +
        "message=${SafeText.text(message)})"
}
