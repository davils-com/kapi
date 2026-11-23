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
import com.davils.kapi.spec.core.policy.ReadLimitsBuilder
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures the comparison of two versions of a document, such as `OpenApi.compare(baseline, candidate) { }`.
 *
 * A builder is not safe to share between threads; the settings it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ComparisonBuilder : ValidatingBuilder<ComparisonSettings>() {
    override val subject: String get() = "comparison"

    /**
     * The limits of the comparison; [ReadLimits.Default] by default. A comparison follows at most
     * [ReadLimits.maximumReferenceHops] references in a row, descends at most [ReadLimits.maximumDepth] schemas deep,
     * takes at most [ReadLimits.maximumNodes] steps and reports at most [ReadLimits.maximumDiagnostics] changes.
     *
     * @since 1.0.0
     */
    public var limits: ReadLimits = ReadLimits.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Configures the [limits] of the comparison.
     *
     * @param configure changes the default limits.
     * @since 1.0.0
     */
    public fun limits(configure: ReadLimitsBuilder.() -> Unit) {
        checkOpen()
        nested(ReadLimitsBuilder(), ::limits, configure)?.let { built -> limits = built }
    }

    override fun assemble(): ComparisonSettings = ComparisonSettings(this)
}
