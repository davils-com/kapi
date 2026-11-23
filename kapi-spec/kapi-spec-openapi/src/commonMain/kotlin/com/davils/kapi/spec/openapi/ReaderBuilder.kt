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

package com.davils.kapi.spec.openapi

import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReadLimitsBuilder
import com.davils.kapi.spec.core.policy.ReferencePolicy
import com.davils.kapi.spec.core.policy.ReferencePolicyBuilder
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.core.policy.SeverityPolicyBuilder
import com.davils.kapi.spec.core.source.Resolver
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures a [Reader]; used by [OpenApi.reader].
 *
 * A builder is not safe to share between threads.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ReaderBuilder internal constructor() : ValidatingBuilder<ReaderConfiguration>() {
    override val subject: String get() = "openapi reader"

    /**
     * Loads the documents of a reading; [Resolver.None] by default, so only the document handed over is read.
     *
     * @since 1.0.0
     */
    public var resolver: Resolver = Resolver.None

    /**
     * How thoroughly the structure is checked; [StructureCheck.ReaderAndOfficialSchema] by default.
     *
     * @since 1.0.0
     */
    public var structure: StructureCheck = StructureCheck.ReaderAndOfficialSchema

    /**
     * Whether the YAML 1.1 merge key `<<` is honoured; `false` by default, which reports it.
     *
     * @since 1.0.0
     */
    public var hasMergeKeys: Boolean = false

    /**
     * The limits of a reading.
     *
     * @since 1.0.0
     */
    public var limits: ReadLimits = ReadLimits.Default

    /**
     * Which documents references may lead to.
     *
     * @since 1.0.0
     */
    public var references: ReferencePolicy = ReferencePolicy.Default

    /**
     * How severe the findings are.
     *
     * @since 1.0.0
     */
    public var severities: SeverityPolicy = SeverityPolicy.Default

    /**
     * Configures [limits], starting from the defaults.
     *
     * @param configure the configuration.
     * @since 1.0.0
     */
    public fun limits(configure: ReadLimitsBuilder.() -> Unit) {
        nested(ReadLimitsBuilder(), ::limits, configure)?.let { limits = it }
    }

    /**
     * Configures [references], starting from the defaults.
     *
     * @param configure the configuration.
     * @since 1.0.0
     */
    public fun references(configure: ReferencePolicyBuilder.() -> Unit) {
        nested(ReferencePolicyBuilder(), ::references, configure)?.let { references = it }
    }

    /**
     * Configures [severities], starting from the defaults.
     *
     * @param configure the configuration.
     * @since 1.0.0
     */
    public fun severities(configure: SeverityPolicyBuilder.() -> Unit) {
        nested(SeverityPolicyBuilder(), ::severities, configure)?.let { severities = it }
    }

    override fun assemble(): ReaderConfiguration = ReaderConfiguration(this)
}
