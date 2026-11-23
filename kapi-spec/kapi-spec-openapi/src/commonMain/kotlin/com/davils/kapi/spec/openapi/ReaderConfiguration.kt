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
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReferencePolicy
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.openapi.policy.StructureCheck
import com.davils.kapi.spec.openapi.diagnostic.CodeRules
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

public class ReaderConfiguration internal constructor(builder: ReaderBuilder) : Validatable {
    public val resolver: Resolver = builder.resolver

    public val structure: StructureCheck = builder.structure

    public val hasMergeKeys: Boolean = builder.hasMergeKeys

    public val limits: ReadLimits = builder.limits

    public val references: ReferencePolicy = builder.references

    public val severities: SeverityPolicy = builder.severities

    override fun ValidationScope.validate() {
        val fixed = severities.downgrades.keys.filterNot { code -> CodeRules.adjustable.any { it.value == code } }
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
