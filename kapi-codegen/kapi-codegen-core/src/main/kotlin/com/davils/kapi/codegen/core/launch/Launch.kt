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

package com.davils.kapi.codegen.core.launch

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.codegen.core.GeneratedPlatform
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.codegen.core.options.Visibility
import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.core.compatibility.Change
import com.davils.kapi.spec.core.compatibility.Comparison
import com.davils.kapi.spec.core.compatibility.Impact
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.Severity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import com.davils.kapi.spec.asyncapi.Reading as ChannelReading

private const val MAXIMUM_MESSAGE_LENGTH: Int = 1024

private const val NO_BASELINE: String = "A comparison needs the baseline of the document"

private val readable: Json = Json { prettyPrint = true }

private val sides: Map<String, List<(Change) -> Impact>> = mapOf(
    "Clients" to listOf(Change::client),
    "Providers" to listOf(Change::provider),
    "Both" to listOf(Change::client, Change::provider),
)

internal class Launch(private val request: LaunchRequest, private val mode: LaunchMode) {
    private val root: Path = request.root.toAbsolutePath().normalize()

    private val readers: LaunchReaders = LaunchReaders(root)

    suspend fun run(): LaunchVerdict {
        val report = checked()
        val verdict = LaunchVerdict.of(report, request.acknowledgements)
        write(request.report, ReportFiles.json(report, verdict))
        write(request.sarif, ReportFiles.sarif(report, request.generatorVersion))
        return verdict
    }

    private suspend fun checked(): LaunchReport {
        val problem = problemOf() ?: comparisonProblemOf()
        if (problem != null) return failed(problem)
        val baseline = request.baseline
        return when (mode) {
            LaunchMode.Generate, LaunchMode.Lint -> read()
            LaunchMode.Compare -> baseline?.let { found -> compared(found) } ?: failed(NO_BASELINE)
        }
    }

    private fun comparisonProblemOf(): String? {
        val isCompared = mode == LaunchMode.Compare
        val baseline = request.baseline?.takeIf { isCompared }
        val absolute = baseline?.toAbsolutePath()
        val outside = absolute?.takeUnless { path -> path.normalize().startsWith(root) }
        val isSideUnknown = isCompared && request.breaking !in sides
        return listOfNotNull(
            outside?.let { path -> "The baseline $path lies outside its root $root" },
            "The side ${request.breaking} is unknown; it is Clients, Providers or Both".takeIf { isSideUnknown },
        ).firstOrNull()
    }

    private fun problemOf(): String? {
        val unknown = request.artifacts.filter { name -> Artifact.entries.none { artifact -> artifact.name == name } }
        val isVisibilityUnknown = Visibility.entries.none { known -> known.name == request.visibility }
        val specification = request.specification.toAbsolutePath()
        val isOutside = !specification.normalize().startsWith(root)
        val isOverlaid = request.kind == LaunchKind.AsyncApi && request.overlays.isNotEmpty()
        return listOfNotNull(
            "The artifacts ${unknown.joinToString()} are unknown".takeIf { unknown.isNotEmpty() },
            "An AsyncAPI document takes no overlays".takeIf { isOverlaid },
            "The visibility ${request.visibility} is unknown".takeIf { isVisibilityUnknown },
            "The document ${request.specification} lies outside its root $root".takeIf { isOutside },
        ).firstOrNull()
    }

    private suspend fun read(): LaunchReport = when (request.kind) {
        LaunchKind.OpenApi -> readOpenApi()
        LaunchKind.AsyncApi -> readAsyncApi()
    }

    private suspend fun readOpenApi(): LaunchReport {
        val reader = readers.openApi()
        val overlays = request.overlays.map { overlay -> Source.Located(PathResolver.uriOf(overlay)) }
        val reading = reader.read(Source.Located(PathResolver.uriOf(request.specification)), overlays)
        return when (reading) {
            is Reading.Read -> generated(reading)
            is Reading.Invalid -> refused(reading.diagnostics)
            is Reading.Malformed -> refused(reading.diagnostics)
        }
    }

    private fun generated(reading: Reading.Read): LaunchReport {
        val generator = try {
            Codegen.openApi {
                packageName = request.packageName
                apiName = request.apiName
                typePrefix = request.typePrefix
                artifacts = request.artifacts.map(Artifact::valueOf).toSet()
                visibility = Visibility.valueOf(request.visibility)
                generatorVersion = request.generatorVersion
            }
        } catch (failure: ValidationException) {
            return failed(failure.message)
        }
        return judged(generator.generate(reading.document, reading.sources), reading.diagnostics)
    }

    private suspend fun readAsyncApi(): LaunchReport {
        val reader = readers.asyncApi()
        val reading = reader.read(Source.Located(PathResolver.uriOf(request.specification)))
        return when (reading) {
            is ChannelReading.Read -> channels(reading)
            is ChannelReading.Invalid -> refused(reading.diagnostics)
            is ChannelReading.Malformed -> refused(reading.diagnostics)
        }
    }

    private suspend fun channels(reading: ChannelReading.Read): LaunchReport {
        val generator = try {
            Codegen.asyncApi {
                packageName = request.packageName
                apiName = request.apiName
                typePrefix = request.typePrefix
                artifacts = request.artifacts.map(Artifact::valueOf).toSet()
                visibility = Visibility.valueOf(request.visibility)
                generatorVersion = request.generatorVersion
            }
        } catch (failure: ValidationException) {
            return failed(failure.message)
        }
        return judged(generator.generate(reading.document, reading.sources), reading.diagnostics)
    }

    private fun judged(generation: Generation, read: List<Diagnostic>): LaunchReport = when (generation) {
        is Generation.Generated -> written(generation, read)
        is Generation.Refused -> refused(read + generation.diagnostics)
    }

    private fun written(generation: Generation.Generated, read: List<Diagnostic>): LaunchReport {
        val isLint = mode == LaunchMode.Lint
        val outcome = LaunchOutcome.Audited.takeIf { isLint } ?: LaunchOutcome.Generated
        if (!isLint) generation.files.forEach(::writeFile)
        return LaunchReport(
            outcome = outcome,
            reason = null,
            diagnostics = (read + generation.diagnostics).map(::diagnosticOf),
            files = generation.files.map(GeneratedFile::path),
        )
    }

    private fun writeFile(file: GeneratedFile) {
        val absolute = folderOf(file).toAbsolutePath()
        val folder = absolute.normalize()
        val target = folder.resolve(file.path).normalize()
        check(target.startsWith(folder)) { "The generated file ${SafeText.quoted(file.path)} leaves its folder" }
        Files.createDirectories(target.parent)
        Files.writeString(target, file.content)
    }

    private fun folderOf(file: GeneratedFile): Path {
        if (file.kind == GeneratedFileKind.Resource) return request.resources
        return request.jvmKotlin.takeIf { file.platform == GeneratedPlatform.Jvm } ?: request.commonKotlin
    }

    private fun refused(found: List<Diagnostic>): LaunchReport = LaunchReport(
        outcome = LaunchOutcome.Refused,
        reason = null,
        diagnostics = found.map(::diagnosticOf),
        files = emptyList(),
    )

    private fun failed(reason: String): LaunchReport = LaunchReport(
        outcome = LaunchOutcome.Failed,
        reason = SafeText.text(reason, MAXIMUM_MESSAGE_LENGTH),
        diagnostics = emptyList(),
        files = emptyList(),
    )

    private suspend fun compared(baseline: Path): LaunchReport = when (request.kind) {
        LaunchKind.OpenApi -> comparedOpenApi(Source.Located(PathResolver.uriOf(baseline)))
        LaunchKind.AsyncApi -> comparedAsyncApi(Source.Located(PathResolver.uriOf(baseline)))
    }

    private suspend fun comparedOpenApi(earliest: Source): LaunchReport {
        val reader = readers.openApi()
        val baseline = reader.read(earliest)
        val candidate = reader.read(Source.Located(PathResolver.uriOf(request.specification)))
        val earlier = baseline as? Reading.Read
        val later = candidate as? Reading.Read
        if (earlier == null || later == null) return refused(baseline.diagnostics + candidate.diagnostics)
        return changed(OpenApi.compare(baseline = earlier.document, candidate = later.document))
    }

    private suspend fun comparedAsyncApi(earliest: Source): LaunchReport {
        val reader = readers.asyncApi()
        val baseline = reader.read(earliest)
        val candidate = reader.read(Source.Located(PathResolver.uriOf(request.specification)))
        val earlier = baseline as? ChannelReading.Read
        val later = candidate as? ChannelReading.Read
        if (earlier == null || later == null) return refused(baseline.diagnostics + candidate.diagnostics)
        return changed(AsyncApi.compare(baseline = earlier.document, candidate = later.document))
    }

    private fun changed(comparison: Comparison): LaunchReport = LaunchReport(
        outcome = LaunchOutcome.Compared,
        reason = null,
        diagnostics = comparison.changes.map(::changeOf),
        files = emptyList(),
    )

    private fun changeOf(change: Change): LaunchDiagnostic {
        val impacts = sides.getValue(request.breaking).map { side -> side(change) }
        return placed(
            code = change.code.value,
            message = change.message,
            location = change.candidate ?: change.baseline,
            isError = false,
            isNote = Impact.Breaking !in impacts,
        )
    }

    private fun diagnosticOf(diagnostic: Diagnostic): LaunchDiagnostic = placed(
        code = diagnostic.code.value,
        message = diagnostic.message,
        location = diagnostic.location,
        isError = diagnostic.severity == Severity.Error,
        isNote = false,
    )

    private fun placed(
        code: String,
        message: String,
        location: Location?,
        isError: Boolean,
        isNote: Boolean,
    ): LaunchDiagnostic {
        val position = location?.position
        return LaunchDiagnostic(
            code = code,
            isError = isError,
            message = SafeText.text(message, MAXIMUM_MESSAGE_LENGTH),
            file = location?.let { found -> relative(found.document.value) },
            line = position?.run { line + 1 },
            column = position?.run { column + 1 },
            isNote = isNote,
        )
    }

    private fun relative(uri: String): String {
        val absolute = Path.of(URI(uri)).toAbsolutePath()
        val path = absolute.normalize()
        val inside = path.takeIf { found -> found.startsWith(root) } ?: return uri
        return root.relativize(inside).joinToString("/")
    }

    private fun write(path: Path, content: JsonObject) {
        Files.createDirectories(path.toAbsolutePath().parent)
        Files.writeString(path, readable.encodeToString(JsonObject.serializer(), content) + "\n")
    }
}
