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

import com.davils.kapi.spec.core.source.Resolver
import com.davils.kapi.spec.openapi.policy.ReadLimits
import com.davils.kapi.spec.openapi.policy.ReadLimitsBuilder
import com.davils.kapi.spec.openapi.policy.ReferencePolicy
import com.davils.kapi.spec.openapi.policy.ReferencePolicyBuilder
import com.davils.kapi.spec.openapi.policy.SeverityPolicy
import com.davils.kapi.spec.openapi.policy.SeverityPolicyBuilder
import com.davils.kapi.spec.openapi.policy.StructureCheck
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

@DavilsDsl
public class ReaderBuilder internal constructor() : ValidatingBuilder<ReaderConfiguration>() {
    override val subject: String get() = "openapi reader"

    public var resolver: Resolver = Resolver.None

    public var structure: StructureCheck = StructureCheck.ReaderAndOfficialSchema

    public var hasMergeKeys: Boolean = false

    public var limits: ReadLimits = ReadLimits.Default

    public var references: ReferencePolicy = ReferencePolicy.Default

    public var severities: SeverityPolicy = SeverityPolicy.Default

    public fun limits(configure: ReadLimitsBuilder.() -> Unit) {
        nested(ReadLimitsBuilder(), ::limits, configure)?.let { limits = it }
    }

    public fun references(configure: ReferencePolicyBuilder.() -> Unit) {
        nested(ReferencePolicyBuilder(), ::references, configure)?.let { references = it }
    }

    public fun severities(configure: SeverityPolicyBuilder.() -> Unit) {
        nested(SeverityPolicyBuilder(), ::severities, configure)?.let { severities = it }
    }

    override fun assemble(): ReaderConfiguration = ReaderConfiguration(this)
}
