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

private const val LAUNCH: String = "kapi-launch"

private const val COMMON: String = "out/common"

private const val LINE: String = "\n"

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

private const val CHAT: String = """asyncapi: 3.0.0
info:
  title: Chat
  version: 1.0.0
defaultContentType: application/json
servers:
  production:
    host: chat.example.com
    protocol: wss
channels:
  room:
    address: /rooms
    messages:
      say:
        ${'$'}ref: '#/components/messages/Say'
operations:
  receiveSay:
    action: receive
    channel:
      ${'$'}ref: '#/channels/room'
    messages:
      - ${'$'}ref: '#/channels/room/messages/say'
components:
  messages:
    Say:
      payload:
        ${'$'}ref: '#/components/schemas/Say'
  schemas:
    Say:
      type: object
      required: [text]
      properties:
        text: {type: string}
"""

private fun chatIn(base: Path, document: String = CHAT, overlays: List<Path> = emptyList()): LaunchRequest {
    val request = requestIn(base, document = document)
    return LaunchRequest(
        specification = request.specification,
        root = request.root,
        overlays = overlays,
        commonKotlin = request.commonKotlin,
        jvmKotlin = request.jvmKotlin,
        resources = request.resources,
        report = request.report,
        sarif = request.sarif,
        packageName = "com.example.chat",
        apiName = "Chat",
        typePrefix = "",
        artifacts = setOf("Models", "Client", "Server"),
        visibility = "Public",
        generatorVersion = "test",
        acknowledgements = emptyMap(),
        kind = LaunchKind.AsyncApi,
    )
}

private fun requestIn(
    base: Path,
    document: String = PETSTORE,
    artifacts: Set<String> = setOf("Models", "Client", "Server"),
    acknowledgements: Map<String, String> = emptyMap(),
    visibility: String = "Public",
    folder: String = "api",
    packageName: String = "com.example.petstore",
): LaunchRequest {
    val root = Files.createDirectories(base.resolve("api"))
    val specification = Files.createDirectories(base.resolve(folder)).resolve("petstore.yaml")
    specification.writeText(document)
    return LaunchRequest(
        specification = specification,
        root = root,
        overlays = emptyList(),
        commonKotlin = base.resolve(COMMON),
        jvmKotlin = base.resolve("out/jvm"),
        resources = base.resolve("out/resources"),
        report = base.resolve("report/report.json"),
        sarif = base.resolve("report/report.sarif"),
        packageName = packageName,
        apiName = "Petstore",
        typePrefix = "",
        artifacts = artifacts,
        visibility = visibility,
        generatorVersion = "test",
        acknowledgements = acknowledgements,
    )
}

private fun request(base: Path): LaunchRequest = requestIn(base)

class LauncherTest : FunSpec({
    context("a run") {
        test("writes common code, jvm code, the provenance and both reports") {
            val base = createTempDirectory(LAUNCH)
            val request = requestIn(base)

            val verdict = Launcher.run(request)

            verdict.isAccepted shouldBe true
            Files.walk(base.resolve(COMMON)).anyMatch { path -> path.toString().endsWith("Pet.kt") } shouldBe true
            Files.walk(base.resolve("out/jvm")).anyMatch { path -> path.toString().contains("server") } shouldBe true
            base.resolve("out/resources/META-INF/kapi/openapi/petstore.provenance.json").exists() shouldBe true
            request.report.readText() shouldContain """"outcome": "Generated""""
            request.sarif.readText() shouldContain """"version": "2.1.0""""
        }

        test("fails on an error of the document and writes nothing but the reports") {
            val base = createTempDirectory(LAUNCH)
            val request = requestIn(
                base,
                document = listOf("openapi: 3.1.0", "info: {title: A, version: '1'}", "paths: {}", "inof: x", "")
                    .joinToString(LINE),
            )

            val verdict = Launcher.run(request)

            verdict.isAccepted shouldBe false
            verdict.failures.joinToString() shouldContain UNKNOWN_FIELD
            base.resolve(COMMON).exists() shouldBe false
            request.sarif.readText() shouldContain UNKNOWN_FIELD
        }

        test("fails before reading on an unknown artifact") {
            val base = createTempDirectory(LAUNCH)

            val verdict = Launcher.run(requestIn(base, artifacts = setOf("Everything")))

            verdict.failures shouldContain "The artifacts Everything are unknown"
        }

        test("refuses a reference that leaves the root") {
            val base = createTempDirectory(LAUNCH)
            base.resolve("secret.yaml").writeText("Pet: {type: object}\n")
            val document = PETSTORE.replace(oldValue = "'#/components/schemas/Pet'", newValue = "'../secret.yaml#/Pet'")

            val verdict = Launcher.run(requestIn(base, document = document))

            verdict.isAccepted shouldBe false
        }
    }

    context("an AsyncAPI run") {
        test("writes the models, the client and the server of the WebSocket channels") {
            val base = createTempDirectory(LAUNCH)
            val request = chatIn(base)

            val verdict = Launcher.run(request)

            verdict.failures.shouldBeEmpty()
            Files.walk(base.resolve(COMMON)).anyMatch { path -> path.toString().endsWith("Say.kt") } shouldBe true
            base.resolve("out/jvm").exists() shouldBe true
            request.toString() shouldContain "AsyncApi"
        }

        test("refuses overlays, a document that is no AsyncAPI and an artifact AsyncAPI does not describe") {
            val base = createTempDirectory(LAUNCH)
            val overlay = base.resolve("api/overlay.yaml")

            Launcher.run(chatIn(base, overlays = listOf(overlay))).failures shouldContain
                "An AsyncAPI document takes no overlays"
            Launcher.run(chatIn(base, document = "asyncapi: [")).isAccepted shouldBe false
            val unknown = CHAT.replace(oldValue = "  room:", newValue = "  room:\n    x: 1")
            Launcher.run(chatIn(base, document = unknown)).isAccepted shouldBe false
        }
    }

    context("a lint") {
        test("writes both reports but no code") {
            val base = createTempDirectory(LAUNCH)
            val request = requestIn(base)

            val verdict = Launcher.lint(request)

            verdict.isAccepted shouldBe true
            base.resolve(COMMON).exists() shouldBe false
            request.report.readText() shouldContain """"outcome": "Audited""""
            request.sarif.exists() shouldBe true
        }

        test("audits an AsyncAPI document and refuses an unused acknowledgement") {
            val base = createTempDirectory(LAUNCH)
            val request = chatIn(base)

            Launcher.lint(request).isAccepted shouldBe true
            base.resolve(COMMON).exists() shouldBe false
            val unused = requestIn(base, acknowledgements = mapOf("openapi.x" to "known"))
            Launcher.lint(unused).failures shouldContain "The acknowledgement of openapi.x matches no warning and has to go"
        }
    }

    context("a run that cannot generate") {
        test("fails before reading on an unknown visibility and on a document outside its root") {
            val base = createTempDirectory(LAUNCH)

            Launcher.run(requestIn(base, visibility = "Secret")).failures shouldContain
                "The visibility Secret is unknown"
            Launcher.run(requestIn(base, folder = "elsewhere")).isAccepted shouldBe false
            Launcher.run(requestIn(base, packageName = "not a package")).isAccepted shouldBe false
        }

        test("refuses a document that is no YAML and one the generator has no Kotlin for") {
            val base = createTempDirectory(LAUNCH)
            val unsupported = PETSTORE.replace(oldValue = "type: object", newValue = "type: [object, string]")

            Launcher.run(requestIn(base, document = "openapi: [")).isAccepted shouldBe false
            Launcher.run(requestIn(base, document = unsupported)).isAccepted shouldBe false
            request(base).report.readText() shouldContain "codegen.schema.unsupported"
        }

        test("reports the warnings of the document with their location in both reports") {
            val base = createTempDirectory(LAUNCH)
            val accept = "      parameters: [{name: Accept, in: header, schema: {type: string}}]\n      responses:"
            val warned = PETSTORE.replaceFirst(oldValue = "      responses:", newValue = accept)

            Launcher.run(requestIn(base, document = warned)).isAccepted shouldBe false
            request(base).sarif.readText() shouldContain """"level": "warning""""
        }
    }

    context("a verdict") {
        test("asks for a reason for every acknowledgement and refuses unused ones") {
            val report = LaunchReport(
                outcome = LaunchOutcome.Generated,
                reason = null,
                diagnostics = emptyList(),
                files = emptyList(),
            )

            val verdict = LaunchVerdict.of(report, mapOf("openapi.a" to "", "openapi.b" to "because"))

            verdict.failures shouldContain "The acknowledgement of openapi.a needs a reason"
            verdict.failures shouldContain "The acknowledgement of openapi.b matches no warning and has to go"
        }

        test("accepts a warning that is acknowledged with a reason") {
            val warning = LaunchDiagnostic(
                code = "openapi.w",
                isError = false,
                message = "m",
                file = null,
                line = null,
                column = null,
            )
            val report = LaunchReport(
                outcome = LaunchOutcome.Generated,
                reason = null,
                diagnostics = listOf(warning),
                files = emptyList(),
            )

            val verdict = LaunchVerdict.of(report, mapOf("openapi.w" to "known"))

            verdict.failures.shouldBeEmpty()
            verdict.lines shouldBe listOf("warning: [openapi.w] m")
        }
    }
})
