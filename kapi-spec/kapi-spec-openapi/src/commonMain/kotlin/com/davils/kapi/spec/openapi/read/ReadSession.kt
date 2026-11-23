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

package com.davils.kapi.spec.openapi.read

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.load.DocumentLoader
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.ReaderConfiguration
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.openapi.Version
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.load.loaderOf
import com.davils.kapi.spec.openapi.overlay.OverlayAction
import com.davils.kapi.spec.openapi.overlay.OverlayApplication
import com.davils.kapi.spec.openapi.overlay.OverlayReader
import com.davils.kapi.spec.openapi.read.reference.TargetResolver
import com.davils.kapi.spec.openapi.validation.DocumentRules
import com.davils.kapi.spec.openapi.validation.RuleScope
import com.davils.kore.core.annotation.DavilsInternal

internal class ReadSession(
    private val configuration: ReaderConfiguration,
    source: Source,
    private val overlays: List<Source>,
) {
    private val findings: Findings = Findings(configuration.severities, configuration.limits.maximumDiagnostics)

    private val loader: DocumentLoader = loaderOf(configuration, findings, source)

    suspend fun run(): Reading {
        val root = loader.root()?.let { loaded -> overlaid(loaded) }
        val version = root?.let { loaded -> VersionReader(loaded, findings).read() }
        val document = version?.let { known -> read(root, known) }
        return outcome(document)
    }

    private suspend fun overlaid(document: LoadedDocument): LoadedDocument {
        if (overlays.isEmpty()) return document
        val actions = overlays.flatMap { overlay -> actionsOf(overlay) }
        val root = OverlayApplication(findings).applied(document.root, actions)
        val applied = LoadedDocument(document.text, root, document.source)
        loader.replace(applied)
        return applied
    }

    private suspend fun actionsOf(overlay: Source): List<OverlayAction> {
        val loaded = loader.overlay(overlay) ?: return emptyList()
        return OverlayReader(loaded, findings).actions()
    }

    private suspend fun read(root: LoadedDocument, version: Version): Document {
        val scope = ReadScope(configuration, version.family, findings)
        val cursor = Cursor(scope = scope, document = root, pointer = JsonPointer.ROOT, node = root.root)
        val builder = DocumentReader.fill(cursor, version)
        builder.targets.putAll(TargetResolver(scope, loader).resolve())
        val document = builder.build()
        val rules = RuleScope(
            document = document,
            limits = configuration.limits,
            findings = findings,
            documents = loader.documents,
        )
        DocumentRules.check(rules)
        return document
    }

    private fun outcome(document: Document?): Reading {
        val diagnostics = findings.sorted(loader.order)
        val sources = (loader.documents + loader.overlays).map(LoadedDocument::source)
        val invalid = document?.takeIf { findings.hasErrors }
        val refused = invalid?.let { partial -> Reading.Invalid(partial, diagnostics, sources) }
        val read = document?.let { complete -> Reading.Read(complete, diagnostics, sources) }
        return refused ?: read ?: Reading.Malformed(diagnostics, sources)
    }
}
