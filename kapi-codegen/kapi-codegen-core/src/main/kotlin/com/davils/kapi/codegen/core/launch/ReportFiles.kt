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
        put("outcome", report.outcome.name)
        put("reason", report.reason)
        put("accepted", verdict.isAccepted)
        put("diagnostics", JsonArray(report.diagnostics.map(::diagnostic)))
        put("files", JsonArray(report.files.map(::JsonPrimitive)))
        put("failures", JsonArray(verdict.failures.map(::JsonPrimitive)))
    }

    fun sarif(report: LaunchReport, version: String): JsonObject = buildJsonObject {
        put("\$schema", SARIF_SCHEMA)
        put("version", SARIF_VERSION)
        putJsonArray("runs") {
            add(run(report, version))
        }
    }

    private fun run(report: LaunchReport, version: String): JsonObject = buildJsonObject {
        putJsonObject("tool") {
            putJsonObject("driver") {
                put("name", TOOL)
                put("version", version)
                put("informationUri", INFORMATION)
                put("rules", JsonArray(report.diagnostics.map { it.code }.distinct().map(::rule)))
            }
        }
        put("results", JsonArray(report.diagnostics.map(::result)))
    }

    private fun rule(code: String): JsonObject = buildJsonObject {
        put("id", code)
    }

    private fun result(diagnostic: LaunchDiagnostic): JsonObject = buildJsonObject {
        put("ruleId", diagnostic.code)
        put("level", "error".takeIf { diagnostic.isError } ?: "warning")
        putJsonObject("message") { put("text", diagnostic.message) }
        put("locations", locations(diagnostic))
    }

    private fun locations(diagnostic: LaunchDiagnostic): JsonElement {
        val file = diagnostic.file ?: return JsonArray(emptyList())
        return buildJsonArray {
            add(
                buildJsonObject {
                    putJsonObject("physicalLocation") {
                        putJsonObject("artifactLocation") { put("uri", file) }
                        put("region", region(diagnostic))
                    }
                },
            )
        }
    }

    private fun region(diagnostic: LaunchDiagnostic): JsonObject = buildJsonObject {
        put("startLine", diagnostic.line ?: 1)
        diagnostic.column?.let { column -> put("startColumn", column) }
    }

    private fun diagnostic(diagnostic: LaunchDiagnostic): JsonObject = buildJsonObject {
        put("code", diagnostic.code)
        put("severity", "error".takeIf { diagnostic.isError } ?: "warning")
        put("message", diagnostic.message)
        put("file", diagnostic.file)
        put("line", diagnostic.line)
        put("column", diagnostic.column)
    }
}
