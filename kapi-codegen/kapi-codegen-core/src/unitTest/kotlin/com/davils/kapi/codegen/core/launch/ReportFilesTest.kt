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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

private val nowhere: LaunchDiagnostic = LaunchDiagnostic(
    code = "codegen.a",
    isError = true,
    message = "m",
    file = null,
    line = null,
    column = null,
)

private val unpositioned: LaunchDiagnostic = LaunchDiagnostic(
    code = "codegen.b",
    isError = false,
    message = "n",
    file = "api/a.yaml",
    line = null,
    column = null,
)

private fun report(diagnostic: LaunchDiagnostic): LaunchReport = LaunchReport(
    outcome = LaunchOutcome.Refused,
    reason = null,
    diagnostics = listOf(diagnostic),
    files = emptyList(),
)

private fun locationsOf(diagnostic: LaunchDiagnostic): JsonArray {
    val sarif = ReportFiles.sarif(report(diagnostic), "1.0.0")
    val runs = sarif.getValue("runs").jsonArray
    val run = runs.single().jsonObject
    val results = run.getValue("results").jsonArray
    val result = results.single().jsonObject
    return result.getValue("locations").jsonArray
}

class ReportFilesTest : FunSpec({
    context("the SARIF log of a finding") {
        test("has no location for a finding that names no file") {
            locationsOf(nowhere) shouldBe JsonArray(emptyList())
        }

        test("starts at the first line of its file for a finding without a position") {
            val location = locationsOf(unpositioned).single().jsonObject
            val physical = location.getValue("physicalLocation").jsonObject
            val region = physical.getValue("region") as JsonObject

            region.keys shouldBe setOf("startLine")
            region.getValue("startLine").toString() shouldBe "1"
        }
    }
})
