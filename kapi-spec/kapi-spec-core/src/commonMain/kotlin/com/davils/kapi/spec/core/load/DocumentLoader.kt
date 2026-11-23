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

package com.davils.kapi.spec.core.load

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.diagnostic.YamlDiagnosticCode
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.read.Findings
import com.davils.kapi.spec.core.source.Refusal
import com.davils.kapi.spec.core.source.Resolution
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.core.source.SourceDocument
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.validation.violation.ViolationCode
import com.davils.krypt.primitive.Crypto
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private val remoteSchemes: Set<String> = setOf("http", "https")

/**
 * Loads the documents of one reading through its resolver: the root, every document a reference names and the
 * overlays, each at most once, within the limits of the reading.
 *
 * A document is refused before it is loaded when it would exceed the number of documents, is remote, has another
 * scheme than the root or lies outside the folder of the references; once loaded, it must fit the size limit, be
 * strict UTF-8 and hold exactly one YAML or JSON document. Every refusal is a finding. A loader is not safe to share
 * between threads.
 *
 * @param settings the resolver, limits and policies of the reading.
 * @param findings the findings of the reading.
 * @param source the root document.
 * @param codes the codes to report with.
 * @param prepare turns a parsed document into the one the reading reads, such as the upgrade of Swagger 2.0; `null`
 * drops the document.
 * @since 1.0.0
 */
@DavilsInternal
public class DocumentLoader(
    private val settings: LoadSettings,
    private val findings: Findings,
    private val source: Source,
    private val codes: LoadCodes,
    private val prepare: (LoadedDocument) -> LoadedDocument? = { document -> document },
) {
    private val yaml: Yaml = YamlSettings.yamlOf(settings.limits, settings.hasMergeKeys)

    private val confinement: Confinement = Confinement.of(settings.references, source.uri)

    private val refusals: Map<Refusal, ViolationCode> = mapOf(
        Refusal.Unresolvable to codes.failed,
        Refusal.Confinement to codes.outside,
        Refusal.Size to codes.size,
    )

    private val reasons: Map<ViolationCode, String> = mapOf(
        codes.tooManyDocuments to "would exceed the number of documents one reading may load",
        codes.remote to "is remote, and a reading loads no document over the network",
        codes.scheme to "has another scheme than the document that refers to it",
        codes.outside to "lies outside the folder references may reach",
        codes.failed to "cannot be resolved by the resolver of the reader",
        codes.size to "is larger than a document may be",
        codes.missing to "does not exist",
        codes.encoding to "is not valid UTF-8",
    )

    private val attempts: MutableMap<DocumentUri, LoadedDocument?> = linkedMapOf()

    private val overlaid: MutableList<LoadedDocument> = mutableListOf()

    /**
     * The documents loaded so far, the root first.
     *
     * @since 1.0.0
     */
    public val documents: List<LoadedDocument> get() = attempts.values.filterNotNull()

    /**
     * The overlays loaded so far, in order.
     *
     * @since 1.0.0
     */
    public val overlays: List<LoadedDocument> get() = overlaid.toList()

    /**
     * The position of every document and overlay in the reading, for sorting findings.
     *
     * @since 1.0.0
     */
    public val order: Map<DocumentUri, Int>
        get() {
            val uris = attempts.keys + overlaid.map(LoadedDocument::uri)
            return uris.withIndex().associate { (index, uri) -> uri to index }
        }

    private val crypto: Crypto = Crypto()

    /**
     * Loads the root document.
     *
     * @return the root document, or `null` if it was refused.
     * @since 1.0.0
     */
    public suspend fun root(): LoadedDocument? {
        val loaded = loadedOf(source)
        attempts[source.uri] = loaded
        return loaded
    }

    /**
     * Loads [overlay].
     *
     * @param overlay the overlay.
     * @return the overlay, or `null` if it was refused.
     * @since 1.0.0
     */
    public suspend fun overlay(overlay: Source): LoadedDocument? = loadedOf(overlay)?.also(overlaid::add)

    /**
     * Replaces the loaded document of the same URI, such as the root after its overlays were applied.
     *
     * @param document the document.
     * @since 1.0.0
     */
    public fun replace(document: LoadedDocument) {
        attempts[document.uri] = document
    }

    private suspend fun loadedOf(origin: Source): LoadedDocument? = when (origin) {
        is Source.Text -> sized(origin.uri, origin.text.encodeToByteString(), null)
        is Source.Located -> fetched(origin.uri, null)
    }

    /**
     * Loads the document [uri] that a reference at [from] names, at most once per reading.
     *
     * @param uri the document.
     * @param from where the reference was written.
     * @return the document, or `null` if it was refused.
     * @since 1.0.0
     */
    public suspend fun load(uri: DocumentUri, from: Location?): LoadedDocument? {
        if (uri in attempts) return attempts[uri]
        val loaded = fetched(uri, from)
        attempts[uri] = loaded
        return loaded
    }

    private suspend fun fetched(uri: DocumentUri, from: Location?): LoadedDocument? {
        val refusal = refusalOf(uri)
        if (refusal != null) return rejected(uri, refusal.first, from)
        val content = contentOf(uri, settings.resolver.resolve(uri), from)
        return content?.let { bytes -> sized(uri, bytes, from) }
    }

    private fun refusalOf(uri: DocumentUri): Pair<ViolationCode, Boolean>? {
        val checks = listOf(
            codes.tooManyDocuments to (attempts.size >= settings.limits.maximumDocuments),
            codes.remote to (uri.scheme.lowercase() in remoteSchemes),
            codes.scheme to (uri.scheme != source.uri.scheme),
            codes.outside to !confinement.contains(uri),
        )
        return checks.firstOrNull { check -> check.second }
    }

    private fun contentOf(uri: DocumentUri, resolution: Resolution, from: Location?): ByteString? = when (resolution) {
        is Resolution.Found -> resolution.content
        Resolution.Missing -> rejected(uri, codes.missing, from)
        is Resolution.Refused -> rejected(uri, refusals.getValue(resolution.refusal), from)
        is Resolution.Failed -> failed(uri, resolution.reason, from)
    }

    private suspend fun sized(uri: DocumentUri, content: ByteString, from: Location?): LoadedDocument? {
        if (content.size > settings.limits.maximumDocumentLength) {
            return rejected(uri, codes.size, from)
        }
        return decoded(uri, content, from)
    }

    private suspend fun decoded(uri: DocumentUri, content: ByteString, from: Location?): LoadedDocument? {
        val text = SourceText.decode(content) ?: return rejected(uri, codes.encoding, from)
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
            findings.report(codes.documentEmpty, "The source holds no document", place)
        }
        return root?.let { node -> prepare(LoadedDocument(text, node, document)) }
    }

    private fun adopt(uri: DocumentUri, lines: LineMap, diagnostic: YamlDiagnostic) {
        val isSeveral = diagnostic.code == YamlDiagnosticCode.TooManyDocuments
        val own = codes.multipleDocuments.takeIf { isSeveral }
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
        findings.report(codes.failed, "The document $uri could not be loaded: $reason", from)
        return null
    }
}
