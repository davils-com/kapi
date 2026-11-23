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

import com.davils.kapi.spec.asyncapi.diagnostic.CodeRules
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReferencePolicy
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.core.source.Resolver
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The configuration of a [Reader], built by [ReaderBuilder].
 *
 * A configuration is immutable and safe to share between threads. A downgrade of a code that guards the reader
 * itself, such as a limit, a load or the confinement of references, is refused with
 * [ReaderViolationCodes.SEVERITY_FIXED].
 *
 * @param builder the builder whose values the configuration takes over.
 * @since 1.0.0
 */
public class ReaderConfiguration internal constructor(builder: ReaderBuilder) : Validatable {
    /**
     * Loads the documents that references name.
     *
     * @since 1.0.0
     */
    public val resolver: Resolver = builder.resolver

    /**
     * How thoroughly the structure is checked.
     *
     * @since 1.0.0
     */
    public val structure: StructureCheck = builder.structure

    /**
     * Whether the merge key `<<` of YAML 1.1 is honoured.
     *
     * @since 1.0.0
     */
    public val hasMergeKeys: Boolean = builder.hasMergeKeys

    /**
     * The limits of a reading.
     *
     * @since 1.0.0
     */
    public val limits: ReadLimits = builder.limits

    /**
     * Which documents references may lead to.
     *
     * @since 1.0.0
     */
    public val references: ReferencePolicy = builder.references

    /**
     * How severe the findings of a reading are.
     *
     * @since 1.0.0
     */
    public val severities: SeverityPolicy = builder.severities

    override fun ValidationScope.validate() {
        val adjustable = CodeRules.adjustable.map { code -> code.value }
        val fixed = severities.downgrades.keys.filterNot { code -> code in adjustable }
        fixed.forEach { code ->
            fail(
                ::severities,
                "downgrades $code, which keeps its severity because it guards the reader itself",
                ReaderViolationCodes.SEVERITY_FIXED,
            )
        }
    }

    override fun toString(): String =
        "ReaderConfiguration(structure=$structure, hasMergeKeys=$hasMergeKeys, limits=$limits, " +
            "references=$references, severities=$severities)"
}
