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

package com.davils.kapi.spec.core.compatibility

import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The settings of a comparison of two versions of a document, built by a [ComparisonBuilder].
 *
 * The settings are immutable and safe to share between threads.
 *
 * @param builder the builder whose state the settings take over.
 * @since 1.0.0
 */
public class ComparisonSettings internal constructor(builder: ComparisonBuilder) : Validatable {
    /**
     * The limits of the comparison.
     *
     * @since 1.0.0
     */
    public val limits: ReadLimits = builder.limits

    override fun ValidationScope.validate(): Unit = Unit

    override fun toString(): String = "ComparisonSettings(limits=$limits)"

    /**
     * Holds the default settings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The settings with the default limits.
         *
         * @since 1.0.0
         */
        public val Default: ComparisonSettings = ComparisonBuilder().build()
    }
}
