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

package com.davils.kapi.spec.openapi.overlay

import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.upgrade.nodeOfLines
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val MAXIMUM_DIAGNOSTICS: Int = 100

internal val overlayUri: DocumentUri = DocumentUri("memory:/api/petstore.overlay.yaml")

internal val overlayHead: List<String> = listOf("overlay: 1.0.0", "info: {title: Fixes, version: 1.0.0}", "actions:")

internal class Overlaid(
    val root: YamlNode,
    val diagnostics: List<Diagnostic>,
) {
    val codes: List<ViolationCode> get() = diagnostics.map(Diagnostic::code)
}

internal fun overlaid(
    document: List<String>,
    overlay: List<String>,
    limits: ReadLimits = ReadLimits.Default,
): Overlaid {
    val findings = Findings(SeverityPolicy.Default, MAXIMUM_DIAGNOSTICS)
    val loaded = loadedOf(overlay)
    val actions = OverlayReader(loaded, findings).actions()
    val root = OverlayApplication(findings, limits).applied(nodeOfLines(document), actions)
    return Overlaid(root, findings.sorted(mapOf(overlayUri to 0)))
}

internal fun actionsOf(vararg actions: String): Overlaid = overlaid(
    document = listOf("a: 1"),
    overlay = overlayHead + actions,
)

private fun loadedOf(lines: List<String>): LoadedDocument {
    val text = yamlOf(lines)
    val bytes = text.encodeToByteString()
    val source = SourceDocument(overlayUri, bytes.size, ByteString())
    return LoadedDocument(text, nodeOfLines(lines), source)
}
