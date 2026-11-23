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

package com.davils.kapi.spec.openapi.load

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.source.Refusal
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.ReaderConfiguration
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.upgrade.SwaggerUpgrade
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.krypt.primitive.Crypto
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private val remoteSchemes: Set<String> = setOf("http", "https")

private val refusals: Map<Refusal, ViolationCode> = mapOf(
    Refusal.Unresolvable to DiagnosticCodes.LOAD_FAILED,
    Refusal.Confinement to DiagnosticCodes.REFERENCE_OUTSIDE,
    Refusal.Size to DiagnosticCodes.LOAD_SIZE,
)

private val reasons: Map<ViolationCode, String> = mapOf(
    DiagnosticCodes.LOAD_DOCUMENTS to "would exceed the number of documents one reading may load",
    DiagnosticCodes.LOAD_REMOTE to "is remote, and a reading loads no document over the network",
    DiagnosticCodes.LOAD_SCHEME to "has another scheme than the document that refers to it",
    DiagnosticCodes.REFERENCE_OUTSIDE to "lies outside the folder references may reach",
    DiagnosticCodes.LOAD_FAILED to "cannot be resolved by the resolver of the reader",
    DiagnosticCodes.LOAD_SIZE to "is larger than a document may be",
    DiagnosticCodes.LOAD_MISSING to "does not exist",
    DiagnosticCodes.SOURCE_ENCODING to "is not valid UTF-8",
)

internal class DocumentLoader(
    private val configuration: ReaderConfiguration,
    private val findings: Findings,
    private val source: Source,
) {
    private val yaml: Yaml = YamlSettings.yamlOf(configuration.limits, configuration.hasMergeKeys)

    private val confinement: Confinement = Confinement.of(configuration.references, source.uri)

    private val upgrade: SwaggerUpgrade = SwaggerUpgrade(findings)

    private val attempts: MutableMap<DocumentUri, LoadedDocument?> = linkedMapOf()

    private val overlaid: MutableList<LoadedDocument> = mutableListOf()

    val documents: List<LoadedDocument> get() = attempts.values.filterNotNull()

    val overlays: List<LoadedDocument> get() = overlaid.toList()

    val order: Map<DocumentUri, Int>
        get() {
            val uris = attempts.keys + overlaid.map(LoadedDocument::uri)
            return uris.withIndex().associate { (index, uri) -> uri to index }
        }

    private val crypto: Crypto = Crypto()

    suspend fun root(): LoadedDocument? {
        val loaded = loadedOf(source)
        attempts[source.uri] = loaded
        return loaded
    }

    suspend fun overlay(overlay: Source): LoadedDocument? = loadedOf(overlay)?.also(overlaid::add)

    fun replace(document: LoadedDocument) {
        attempts[document.uri] = document
    }

    private suspend fun loadedOf(origin: Source): LoadedDocument? = when (origin) {
        is Source.Text -> sized(origin.uri, origin.text.encodeToByteString(), null)
        is Source.Located -> fetched(origin.uri, null)
    }

    suspend fun load(uri: DocumentUri, from: Location?): LoadedDocument? {
        if (uri in attempts) return attempts[uri]
        val loaded = fetched(uri, from)
        attempts[uri] = loaded
        return loaded
    }

    private suspend fun fetched(uri: DocumentUri, from: Location?): LoadedDocument? {
        val refusal = refusalOf(uri)
        if (refusal != null) return rejected(uri, refusal.first, from)
        val content = contentOf(uri, configuration.resolver.resolve(uri), from)
        return content?.let { bytes -> sized(uri, bytes, from) }
    }

    private fun refusalOf(uri: DocumentUri): Pair<ViolationCode, Boolean>? {
        val checks = listOf(
            DiagnosticCodes.LOAD_DOCUMENTS to (attempts.size >= configuration.limits.maximumDocuments),
            DiagnosticCodes.LOAD_REMOTE to (uri.scheme.lowercase() in remoteSchemes),
            DiagnosticCodes.LOAD_SCHEME to (uri.scheme != source.uri.scheme),
            DiagnosticCodes.REFERENCE_OUTSIDE to !confinement.contains(uri),
        )
        return checks.firstOrNull { check -> check.second }
    }

    private fun contentOf(uri: DocumentUri, resolution: Resolution, from: Location?): ByteString? = when (resolution) {
        is Resolution.Found -> resolution.content
        Resolution.Missing -> rejected(uri, DiagnosticCodes.LOAD_MISSING, from)
        is Resolution.Refused -> rejected(uri, refusals.getValue(resolution.refusal), from)
        is Resolution.Failed -> failed(uri, resolution.reason, from)
    }

    private suspend fun sized(uri: DocumentUri, content: ByteString, from: Location?): LoadedDocument? {
        if (content.size > configuration.limits.maximumDocumentLength) {
            return rejected(uri, DiagnosticCodes.LOAD_SIZE, from)
        }
        return decoded(uri, content, from)
    }

    private suspend fun decoded(uri: DocumentUri, content: ByteString, from: Location?): LoadedDocument? {
        val text = SourceText.decode(content) ?: return rejected(uri, DiagnosticCodes.SOURCE_ENCODING, from)
        return parsed(text, SourceDocument(uri, content.size, crypto.digests.digest(content)))
    }

    private fun parsed(text: String, document: SourceDocument): LoadedDocument? {
        val result = yaml.parse(text)
        val lines = LineMap.of(text)
        result.diagnostics.forEach { diagnostic -> adopt(document.uri, lines, diagnostic) }
        val stream = (result as? YamlParseResult.Parsed)?.stream
        val root = stream?.single?.root
        if (stream != null && root == null) {
            val place = Location(document.uri, JsonPointer.ROOT)
            findings.report(DiagnosticCodes.DOCUMENT_NOT_MAPPING, "The source holds no document", place)
        }
        return root?.let { node -> upgrade.upgraded(LoadedDocument(text, node, document)) }
    }

    private fun adopt(uri: DocumentUri, lines: LineMap, diagnostic: YamlDiagnostic) {
        val isSeveral = diagnostic.code == YamlDiagnosticCode.TooManyDocuments
        val own = DiagnosticCodes.SOURCE_MULTIPLE_DOCUMENTS.takeIf { isSeveral }
        val position = lines.positionOf(diagnostic.range.start)
        findings.adopt(
            code = own ?: ViolationCode(diagnostic.code.value),
            base = diagnostic.severity,
            message = diagnostic.message,
            location = Location(
                document = uri,
                pointer = JsonPointer.ROOT,
                range = diagnostic.range,
                position = position,
            ),
            snippet = lines.snippet(diagnostic.range),
        )
    }

    private fun <T> rejected(uri: DocumentUri, code: ViolationCode, from: Location?): T? {
        findings.report(code, "The document $uri ${reasons.getValue(code)}", from)
        return null
    }

    private fun <T> failed(uri: DocumentUri, reason: String, from: Location?): T? {
        findings.report(DiagnosticCodes.LOAD_FAILED, "The document $uri could not be loaded: $reason", from)
        return null
    }
}
