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

import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private const val SARIF_SCHEMA: String = "https://json.schemastore.org/sarif-2.1.0.json"

private const val SARIF_VERSION: String = "2.1.0"

private const val TOOL: String = "kapi"

private const val INFORMATION: String = "https://github.com/davils-com/kapi"

internal object ReportFiles {
    fun json(report: LaunchReport, verdict: LaunchVerdict): JsonObject = buildJsonObject {
        put(key = "outcome", value = report.outcome.name)
        put(key = "reason", value = report.reason)
        put(key = "accepted", value = verdict.isAccepted)
        put(key = "diagnostics", element = JsonArray(report.diagnostics.map(::diagnostic)))
        put(key = "files", element = JsonArray(report.files.map(::JsonPrimitive)))
        put(key = "failures", element = JsonArray(verdict.failures.map(::JsonPrimitive)))
    }

    fun sarif(report: LaunchReport, version: String): JsonObject = buildJsonObject {
        put(key = "\$schema", value = SARIF_SCHEMA)
        put(key = "version", value = SARIF_VERSION)
        putJsonArray("runs") {
            add(run(report, version))
        }
    }

    private fun run(report: LaunchReport, version: String): JsonObject = buildJsonObject {
        putJsonObject("tool") {
            putJsonObject("driver") {
                put(key = "name", value = TOOL)
                put(key = "version", value = version)
                put(key = "informationUri", value = INFORMATION)
                val codes = report.diagnostics.map { diagnostic -> diagnostic.code }
                put(key = "rules", element = JsonArray(codes.distinct().map(::rule)))
            }
        }
        put(key = "results", element = JsonArray(report.diagnostics.map(::result)))
    }

    private fun rule(code: String): JsonObject = buildJsonObject {
        put(key = "id", value = code)
    }

    private fun result(diagnostic: LaunchDiagnostic): JsonObject = buildJsonObject {
        put(key = "ruleId", value = diagnostic.code)
        put(key = "level", value = levelOf(diagnostic))
        putJsonObject("message") { put(key = "text", value = diagnostic.message) }
        put(key = "locations", element = locations(diagnostic))
    }

    private fun locations(diagnostic: LaunchDiagnostic): JsonElement {
        val file = diagnostic.file ?: return JsonArray(emptyList())
        return buildJsonArray {
            add(
                buildJsonObject {
                    putJsonObject("physicalLocation") {
                        putJsonObject("artifactLocation") { put(key = "uri", value = file) }
                        put(key = "region", element = region(diagnostic))
                    }
                },
            )
        }
    }

    private fun levelOf(diagnostic: LaunchDiagnostic): String =
        "error".takeIf { diagnostic.isError } ?: "note".takeIf { diagnostic.isNote } ?: "warning"

    private fun region(diagnostic: LaunchDiagnostic): JsonObject = buildJsonObject {
        put(key = "startLine", value = diagnostic.line ?: 1)
        diagnostic.column?.let { column -> put(key = "startColumn", value = column) }
    }

    private fun diagnostic(diagnostic: LaunchDiagnostic): JsonObject = buildJsonObject {
        put(key = "code", value = diagnostic.code)
        put(key = "severity", value = levelOf(diagnostic))
        put(key = "message", value = diagnostic.message)
        put(key = "file", value = diagnostic.file)
        put(key = "line", value = diagnostic.line)
        put(key = "column", value = diagnostic.column)
    }
}
