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

package com.davils.kapi.spec.openapi.policy

import com.davils.kaml.core.limit.YamlLimits
import com.davils.kapi.spec.openapi.ReaderViolationCodes
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty0

public class ReadLimits internal constructor(builder: ReadLimitsBuilder) : Validatable {
    public val maximumDocumentLength: Int = builder.maximumDocumentLength

    public val maximumDocuments: Int = builder.maximumDocuments

    public val maximumDepth: Int = builder.maximumDepth

    public val maximumNodes: Long = builder.maximumNodes

    public val maximumAliasExpansion: Long = builder.maximumAliasExpansion

    public val maximumReferenceHops: Int = builder.maximumReferenceHops

    public val maximumScalarLength: Int = builder.maximumScalarLength

    public val maximumPatternLength: Int = builder.maximumPatternLength

    public val maximumDiagnostics: Int = builder.maximumDiagnostics

    override fun ValidationScope.validate() {
        positive(::maximumDocumentLength, maximumDocumentLength.toLong())
        positive(::maximumDocuments, maximumDocuments.toLong())
        positive(::maximumDepth, maximumDepth.toLong())
        positive(::maximumNodes, maximumNodes)
        positive(::maximumAliasExpansion, maximumAliasExpansion)
        positive(::maximumReferenceHops, maximumReferenceHops.toLong())
        positive(::maximumScalarLength, maximumScalarLength.toLong())
        positive(::maximumPatternLength, maximumPatternLength.toLong())
        positive(::maximumDiagnostics, maximumDiagnostics.toLong())
        if (maximumDepth > YamlLimits.HARD_DEPTH) {
            fail(
                ::maximumDepth,
                "must not exceed ${YamlLimits.HARD_DEPTH}, the depth the parser can nest safely",
                ReaderViolationCodes.ABOVE_HARD_LIMIT,
            )
        }
    }

    override fun toString(): String =
        "ReadLimits(document=$maximumDocumentLength, documents=$maximumDocuments, depth=$maximumDepth, " +
            "nodes=$maximumNodes, aliasExpansion=$maximumAliasExpansion, referenceHops=$maximumReferenceHops, " +
            "scalar=$maximumScalarLength, pattern=$maximumPatternLength, diagnostics=$maximumDiagnostics)"

    private fun ValidationScope.positive(property: KProperty0<*>, value: Long) {
        if (value < 1) {
            fail(
                property,
                "must be at least 1, but was $value",
                ReaderViolationCodes.NOT_POSITIVE,
            )
        }
    }

    public companion object {
        public val Default: ReadLimits = ReadLimitsBuilder().build()
    }
}
