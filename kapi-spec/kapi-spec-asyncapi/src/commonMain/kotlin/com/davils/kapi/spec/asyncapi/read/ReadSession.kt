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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.ReaderConfiguration
import com.davils.kapi.spec.asyncapi.Reading
import com.davils.kapi.spec.asyncapi.StructureCheck
import com.davils.kapi.spec.asyncapi.Version
import com.davils.kapi.spec.asyncapi.diagnostic.Findings
import com.davils.kapi.spec.asyncapi.load.loaderOf
import com.davils.kapi.spec.asyncapi.read.reference.TargetResolver
import com.davils.kapi.spec.asyncapi.structure.OfficialSchema
import com.davils.kapi.spec.asyncapi.structure.StructureValidation
import com.davils.kapi.spec.asyncapi.validation.DocumentRules
import com.davils.kapi.spec.asyncapi.validation.RuleScope
import com.davils.kapi.spec.core.load.DocumentLoader
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.source.Source
import com.davils.kore.core.annotation.DavilsInternal

internal class ReadSession(
    private val configuration: ReaderConfiguration,
    private val schema: OfficialSchema,
    source: Source,
) {
    private val findings: Findings = Findings(configuration.severities, configuration.limits.maximumDiagnostics)

    private val loader: DocumentLoader = loaderOf(configuration, findings, source)

    suspend fun run(): Reading {
        val root = loader.root()
        val version = root?.let { loaded -> VersionReader(loaded, findings).read() }
        val document = documentOf(root, version)
        return outcome(document)
    }

    private suspend fun documentOf(root: LoadedDocument?, version: Version?): Document? {
        if (root == null || version == null) return null
        return read(root, version)
    }

    private suspend fun read(root: LoadedDocument, version: Version): Document {
        val scope = ReadScope(configuration, version.family, findings)
        val cursor = Cursor(scope = scope, document = root, pointer = JsonPointer.ROOT, node = root.root)
        val builder = DocumentReader.fill(cursor, version)
        builder.targets.putAll(TargetResolver(scope, loader).resolve())
        val document = builder.build()
        val rules = RuleScope(document = document, findings = findings, documents = loader.documents)
        DocumentRules.check(rules)
        if (configuration.structure == StructureCheck.ReaderAndOfficialSchema) {
            StructureValidation(findings = findings, document = root, model = document).check(schema)
        }
        return document
    }

    private fun outcome(document: Document?): Reading {
        val diagnostics = findings.sorted(loader.order)
        val sources = loader.documents.map(LoadedDocument::source)
        val invalid = document?.takeIf { findings.hasErrors }
        val refused = invalid?.let { partial -> Reading.Invalid(partial, diagnostics, sources) }
        val read = document?.let { complete -> Reading.Read(complete, diagnostics, sources) }
        return refused ?: read ?: Reading.Malformed(diagnostics, sources)
    }
}
