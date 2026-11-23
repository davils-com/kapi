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

package com.davils.kapi.spec.asyncapi

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
 * Configures a [Reader]: the resolver of referenced documents, the limits, the reference policy and the severities.
 *
 * Every setting starts at a default that suits documents from an untrusted source. A builder is not safe to share
 * between threads; the configuration it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ReaderBuilder internal constructor() : ValidatingBuilder<ReaderConfiguration>() {
    override val subject: String get() = "asyncapi reader"

    /**
     * Loads the documents that references name; [Resolver.None] follows only references inside the document.
     *
     * @since 1.0.0
     */
    public var resolver: Resolver = Resolver.None
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * How thoroughly the structure is checked; [StructureCheck.ReaderAndOfficialSchema] by default.
     *
     * @since 1.0.0
     */
    public var structure: StructureCheck = StructureCheck.ReaderAndOfficialSchema
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Whether the merge key `<<` of YAML 1.1 is honoured; off by default, so a merge key is a finding.
     *
     * @since 1.0.0
     */
    public var hasMergeKeys: Boolean = false
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The limits of a reading.
     *
     * @since 1.0.0
     */
    public var limits: ReadLimits = ReadLimits.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Which documents references may lead to.
     *
     * @since 1.0.0
     */
    public var references: ReferencePolicy = ReferencePolicy.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * How severe the findings of a reading are.
     *
     * @since 1.0.0
     */
    public var severities: SeverityPolicy = SeverityPolicy.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Configures the [limits].
     *
     * @param configure sets the limits.
     * @since 1.0.0
     */
    public fun limits(configure: ReadLimitsBuilder.() -> Unit) {
        checkOpen()
        nested(ReadLimitsBuilder(), ::limits, configure)?.let { built -> limits = built }
    }

    /**
     * Configures the [references].
     *
     * @param configure sets the reference policy.
     * @since 1.0.0
     */
    public fun references(configure: ReferencePolicyBuilder.() -> Unit) {
        checkOpen()
        nested(ReferencePolicyBuilder(), ::references, configure)?.let { built -> references = built }
    }

    /**
     * Configures the [severities].
     *
     * @param configure sets the severity policy.
     * @since 1.0.0
     */
    public fun severities(configure: SeverityPolicyBuilder.() -> Unit) {
        checkOpen()
        nested(SeverityPolicyBuilder(), ::severities, configure)?.let { built -> severities = built }
    }

    override fun assemble(): ReaderConfiguration = ReaderConfiguration(this)
}
