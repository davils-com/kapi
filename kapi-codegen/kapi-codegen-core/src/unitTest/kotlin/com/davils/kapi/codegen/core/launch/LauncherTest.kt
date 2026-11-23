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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val PETSTORE: String = """openapi: 3.1.0
info:
  title: Petstore
  version: 1.0.0
paths:
  /pets:
    get:
      operationId: listPets
      responses:
        "200":
          description: The pets
          content:
            application/json:
              schema:
                type: array
                items: {${'$'}ref: '#/components/schemas/Pet'}
components:
  schemas:
    Pet:
      type: object
      required: [name]
      properties:
        name: {type: string}
"""

private const val UNKNOWN_FIELD: String = "openapi.field.unknown"

private fun requestIn(
    base: Path,
    document: String = PETSTORE,
    artifacts: Set<String> = setOf("Models", "Client", "Server"),
    acknowledgements: Map<String, String> = emptyMap(),
): LaunchRequest {
    val root = Files.createDirectories(base.resolve("api"))
    val specification = root.resolve("petstore.yaml")
    specification.writeText(document)
    return LaunchRequest(
        specification = specification,
        root = root,
        overlays = emptyList(),
        commonKotlin = base.resolve("out/common"),
        jvmKotlin = base.resolve("out/jvm"),
        resources = base.resolve("out/resources"),
        report = base.resolve("report/report.json"),
        sarif = base.resolve("report/report.sarif"),
        packageName = "com.example.petstore",
        apiName = "Petstore",
        typePrefix = "",
        artifacts = artifacts,
        visibility = "Public",
        generatorVersion = "test",
        acknowledgements = acknowledgements,
    )
}

class LauncherTest : FunSpec({
    context("a run") {
        test("writes common code, jvm code, the provenance and both reports") {
            val base = createTempDirectory("kapi-launch")
            val request = requestIn(base)

            val verdict = Launcher.run(request)

            verdict.isAccepted shouldBe true
            Files.walk(base.resolve("out/common")).anyMatch { path -> path.toString().endsWith("Pet.kt") } shouldBe true
            Files.walk(base.resolve("out/jvm")).anyMatch { path -> path.toString().contains("server") } shouldBe true
            base.resolve("out/resources/META-INF/kapi/openapi/petstore.provenance.json").exists() shouldBe true
            request.report.readText() shouldContain "\"outcome\": \"Generated\""
            request.sarif.readText() shouldContain "\"version\": \"2.1.0\""
        }

        test("fails on an error of the document and writes nothing but the reports") {
            val base = createTempDirectory("kapi-launch")
            val request = requestIn(base, document = "openapi: 3.1.0\ninfo: {title: A, version: '1'}\npaths: {}\ninof: x\n")

            val verdict = Launcher.run(request)

            verdict.isAccepted shouldBe false
            verdict.failures.joinToString() shouldContain UNKNOWN_FIELD
            base.resolve("out/common").exists() shouldBe false
            request.sarif.readText() shouldContain UNKNOWN_FIELD
        }

        test("fails before reading on an unknown artifact") {
            val base = createTempDirectory("kapi-launch")

            val verdict = Launcher.run(requestIn(base, artifacts = setOf("Everything")))

            verdict.failures shouldContain "The artifacts Everything are unknown"
        }

        test("refuses a reference that leaves the root") {
            val base = createTempDirectory("kapi-launch")
            base.resolve("secret.yaml").writeText("Pet: {type: object}\n")
            val document = PETSTORE.replace("'#/components/schemas/Pet'", "'../secret.yaml#/Pet'")

            val verdict = Launcher.run(requestIn(base, document = document))

            verdict.isAccepted shouldBe false
        }
    }

    context("a verdict") {
        test("asks for a reason for every acknowledgement and refuses unused ones") {
            val report = LaunchReport(LaunchOutcome.Generated, null, emptyList(), emptyList())

            val verdict = LaunchVerdict.of(report, mapOf("openapi.a" to "", "openapi.b" to "because"))

            verdict.failures shouldContain "The acknowledgement of openapi.a needs a reason"
            verdict.failures shouldContain "The acknowledgement of openapi.b matches no warning and has to go"
        }

        test("accepts a warning that is acknowledged with a reason") {
            val warning = LaunchDiagnostic("openapi.w", isError = false, message = "m", file = null, line = null, column = null)
            val report = LaunchReport(LaunchOutcome.Generated, null, listOf(warning), emptyList())

            val verdict = LaunchVerdict.of(report, mapOf("openapi.w" to "known"))

            verdict.failures.shouldBeEmpty()
            verdict.lines shouldBe listOf("warning: [openapi.w] m")
        }
    }
})
