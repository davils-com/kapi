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

package com.davils.kapi.spec.openapi.load.fixture

import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.ReaderBuilder
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.fixture.configuration
import com.davils.kapi.spec.core.load.DocumentLoader
import com.davils.kapi.spec.openapi.load.loaderOf
import com.davils.kapi.spec.openapi.testing.MemoryResolver
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.kore.core.annotation.DavilsInternal

internal class Loading(
    source: Source,
    documents: Map<DocumentUri, String> = emptyMap(),
    answers: Map<DocumentUri, Resolution> = emptyMap(),
    configure: ReaderBuilder.() -> Unit = {},
) {
    val resolver: MemoryResolver = MemoryResolver(documents, answers)

    private val configuration = configuration {
        resolver = this@Loading.resolver
        configure()
    }

    private val findings: Findings = Findings(configuration.severities, configuration.limits.maximumDiagnostics)

    val loader: DocumentLoader = loaderOf(configuration, findings, source)

    val diagnostics: List<Diagnostic> get() = findings.sorted(loader.order)

    val codes: List<ViolationCode> get() = diagnostics.map(Diagnostic::code)
}
