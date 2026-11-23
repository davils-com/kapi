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

package com.davils.kapi.runtime.server.problem

import com.davils.kapi.runtime.server.ServerViolationCodes
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private const val MOST_TYPE_BASE_LENGTH: Int = 1_024

/**
 * How a server writes the problem details of RFC 9457 it answers with.
 *
 * Without [typeBase] every problem has the type `about:blank` and is told apart by its status and its member `code`;
 * with it, the type of a problem is [typeBase] followed by its code, such as
 * `https://api.example.com/problems/kapi.server.too-large`, a URI a client may resolve to documentation. Every
 * problem keeps its texts free of what the request carried and of the failures of handlers. The settings are
 * immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class ProblemSettings internal constructor(builder: ProblemSettingsBuilder) : Validatable {
    /**
     * The URI every type of a problem starts with, or `null` for `about:blank`.
     *
     * @since 1.0.0
     */
    public val typeBase: String? = builder.typeBase

    override fun ValidationScope.validate() {
        val base = typeBase ?: return
        val isVisible = base.all { char -> char in '!'..'~' && char != '"' && char != '\\' }
        if (base.isEmpty() || base.length > MOST_TYPE_BASE_LENGTH || !isVisible) {
            fail(::typeBase, "must be a URI of visible ASCII characters", ServerViolationCodes.TYPE_BASE_INVALID)
        }
    }

    @OptIn(DavilsInternal::class)
    override fun toString(): String = "ProblemSettings(typeBase=${typeBase?.let(SafeText::text) ?: "none"})"

    /**
     * The default settings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The settings whose problems have the type `about:blank`.
         *
         * @since 1.0.0
         */
        public val Default: ProblemSettings = ProblemSettingsBuilder().build()
    }
}
