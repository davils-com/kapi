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

package com.davils.kapi.spec.openapi.load

import com.davils.kapi.spec.core.load.DocumentLoader
import com.davils.kapi.spec.core.load.LoadSettings
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.ReaderConfiguration
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.upgrade.SwaggerUpgrade
import com.davils.kore.core.annotation.DavilsInternal

internal fun loaderOf(configuration: ReaderConfiguration, findings: Findings, source: Source): DocumentLoader {
    val settings = LoadSettings(
        resolver = configuration.resolver,
        limits = configuration.limits,
        references = configuration.references,
        hasMergeKeys = configuration.hasMergeKeys,
    )
    val upgrade = SwaggerUpgrade(findings)
    return DocumentLoader(
        settings = settings,
        findings = findings,
        source = source,
        codes = LoadDiagnostics,
        prepare = upgrade::upgraded,
    )
}
