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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

@DavilsDsl
public class ReadLimitsBuilder internal constructor() : ValidatingBuilder<ReadLimits>() {
    override val subject: String get() = "openapi limits"

    public var maximumDocumentLength: Int = DEFAULT_DOCUMENT_LENGTH

    public var maximumDocuments: Int = DEFAULT_DOCUMENTS

    public var maximumDepth: Int = DEFAULT_DEPTH

    public var maximumNodes: Long = DEFAULT_NODES

    public var maximumAliasExpansion: Long = DEFAULT_ALIAS_EXPANSION

    public var maximumReferenceHops: Int = DEFAULT_REFERENCE_HOPS

    public var maximumScalarLength: Int = DEFAULT_SCALAR_LENGTH

    public var maximumPatternLength: Int = DEFAULT_PATTERN_LENGTH

    public var maximumDiagnostics: Int = DEFAULT_DIAGNOSTICS

    override fun assemble(): ReadLimits = ReadLimits(this)

    public companion object {
        public const val DEFAULT_DOCUMENT_LENGTH: Int = 16 * 1024 * 1024
        public const val DEFAULT_DOCUMENTS: Int = 64
        public const val DEFAULT_DEPTH: Int = 128
        public const val DEFAULT_NODES: Long = 2_000_000L
        public const val DEFAULT_ALIAS_EXPANSION: Long = 100_000L
        public const val DEFAULT_REFERENCE_HOPS: Int = 32
        public const val DEFAULT_SCALAR_LENGTH: Int = 1024 * 1024
        public const val DEFAULT_PATTERN_LENGTH: Int = 1024
        public const val DEFAULT_DIAGNOSTICS: Int = 1000
    }
}
