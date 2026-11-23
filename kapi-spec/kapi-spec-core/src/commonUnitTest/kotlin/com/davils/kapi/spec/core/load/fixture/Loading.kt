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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.core.load.fixture

import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.load.DocumentLoader
import com.davils.kapi.spec.core.load.LoadSettings
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.ReferencePolicy
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.core.read.Findings
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Resolver
import com.davils.kapi.spec.core.source.Source
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.Severity
import com.davils.kore.core.validation.violation.ViolationCode

private const val MAXIMUM_DIAGNOSTICS: Int = 100

internal class Loading(
    source: Source,
    answers: Map<DocumentUri, Resolution> = emptyMap(),
    limits: ReadLimits = ReadLimits.Default,
    references: ReferencePolicy = ReferencePolicy.Default,
    hasMergeKeys: Boolean = false,
    prepare: (LoadedDocument) -> LoadedDocument? = { document -> document },
) {
    val requested: MutableList<DocumentUri> = mutableListOf()

    private val resolver: Resolver = Resolver { uri ->
        requested += uri
        answers[uri] ?: Resolution.Missing
    }

    private val findings: Findings = Findings(
        policy = SeverityPolicy.Default,
        maximum = MAXIMUM_DIAGNOSTICS,
        overflow = ViolationCode("test.overflow"),
        baseSeverity = { Severity.Error },
    )

    val loader: DocumentLoader = DocumentLoader(
        settings = LoadSettings(
            resolver = resolver,
            limits = limits,
            references = references,
            hasMergeKeys = hasMergeKeys,
        ),
        findings = findings,
        source = source,
        codes = TestLoadCodes,
        prepare = prepare,
    )

    val diagnostics: List<Diagnostic> get() = findings.sorted(loader.order)

    val codes: List<ViolationCode> get() = diagnostics.map(Diagnostic::code)
}
