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
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.violation.Severity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

private const val MAXIMUM_DOCUMENT_LENGTH: Int = 16 * 1024 * 1024

private const val MAXIMUM_MESSAGE_LENGTH: Int = 1024

private val readable: Json = Json { prettyPrint = true }

internal class Launch(private val request: LaunchRequest) {
    private val root: Path = request.root.toAbsolutePath().normalize()

    suspend fun run(): LaunchVerdict {
        val report = checked()
        val verdict = LaunchVerdict.of(report, request.acknowledgements)
        write(request.report, ReportFiles.json(report, verdict))
        write(request.sarif, ReportFiles.sarif(report, request.generatorVersion))
        return verdict
    }

    private suspend fun checked(): LaunchReport {
        val problem = problemOf()
        if (problem != null) return failed(problem)
        return read()
    }

    private fun problemOf(): String? {
        val unknown = request.artifacts.filter { name -> Artifact.entries.none { artifact -> artifact.name == name } }
        val isVisibilityUnknown = Visibility.entries.none { known -> known.name == request.visibility }
        val specification = request.specification.toAbsolutePath()
        val isOutside = !specification.normalize().startsWith(root)
        return listOfNotNull(
            "The artifacts ${unknown.joinToString()} are unknown".takeIf { unknown.isNotEmpty() },
            "The visibility ${request.visibility} is unknown".takeIf { isVisibilityUnknown },
            "The document ${request.specification} lies outside its root $root".takeIf { isOutside },
        ).firstOrNull()
    }

    private suspend fun read(): LaunchReport {
        val reader = OpenApi.reader {
            resolver = PathResolver(root, MAXIMUM_DOCUMENT_LENGTH)
            references { this.root = PathResolver.uriOf(this@Launch.root.resolve(".")) }
        }
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
        } catch (failure: IllegalArgumentException) {
            return failed(failure.message.orEmpty())
        }
        return when (val generation = generator.generate(reading.document, reading.sources)) {
            is Generation.Generated -> written(generation, reading.diagnostics)
            is Generation.Refused -> refused(reading.diagnostics + generation.diagnostics)
        }
    }

    private fun written(generation: Generation.Generated, read: List<Diagnostic>): LaunchReport {
        generation.files.forEach(::writeFile)
        return LaunchReport(
            outcome = LaunchOutcome.Generated,
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

    private fun diagnosticOf(diagnostic: Diagnostic): LaunchDiagnostic {
        val location = diagnostic.location
        val position = location?.position
        return LaunchDiagnostic(
            code = diagnostic.code.value,
            isError = diagnostic.severity == Severity.Error,
            message = SafeText.text(diagnostic.message, MAXIMUM_MESSAGE_LENGTH),
            file = location?.let { found -> relative(found.document.value) },
            line = position?.let { found -> found.line + 1 },
            column = position?.let { found -> found.column + 1 },
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
