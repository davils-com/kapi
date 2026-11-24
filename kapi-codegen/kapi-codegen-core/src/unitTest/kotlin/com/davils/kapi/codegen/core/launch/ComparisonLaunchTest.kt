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
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val COMPARE: String = "kapi-compare"

private const val REMOVED: String = "openapi.compatibility.operation-removed"

private const val ADDED: String = "openapi.compatibility.operation-added"

private const val REASON: String = "pets are listed by the search from 2.0.0 on"

private const val NEWLINE: String = "\n"

private const val NO_CONTENT: String = "{'204': {description: ok}}"

private fun openApi(vararg operations: String): String {
    val head = listOf("openapi: 3.1.0", "info: {title: Petstore, version: 1.0.0}", "paths:", "  /pets:")
    val declared = operations.map { method -> "    $method: {operationId: ${method}Pets, responses: $NO_CONTENT}" }
    return (head + declared + "").joinToString(NEWLINE)
}

private fun asyncApi(vararg channels: String): String {
    val head = listOf("asyncapi: 3.0.0", "info: {title: Chat, version: 1.0.0}", "channels:")
    val declared = channels.map { channel -> "  $channel: {address: /$channel}" }
    return (head + declared + "").joinToString(NEWLINE)
}

private fun requestOf(base: Path, versions: Versions): LaunchRequest {
    val root = Files.createDirectories(base.resolve("api"))
    val specification = root.resolve("petstore.yaml")
    specification.writeText(versions.candidate)
    val folder = Files.createDirectories(base.resolve("baseline".takeUnless { versions.isBaselineInside } ?: "api"))
    val baseline = folder.resolve("petstore-1.0.0.yaml")
    baseline.writeText(versions.baseline)
    return LaunchRequest(
        specification = specification,
        root = root,
        overlays = emptyList(),
        commonKotlin = base.resolve("out/common"),
        jvmKotlin = base.resolve("out/jvm"),
        resources = base.resolve("out/resources"),
        report = base.resolve("report/compatibility.json"),
        sarif = base.resolve("report/compatibility.sarif"),
        packageName = "com.example.petstore",
        apiName = "Petstore",
        typePrefix = "",
        artifacts = setOf("Models"),
        visibility = "Public",
        generatorVersion = "test",
        acknowledgements = versions.acknowledgements,
        kind = versions.kind,
        baseline = baseline,
        breaking = versions.breaking,
    )
}

private fun compared(versions: Versions): Pair<LaunchRequest, LaunchVerdict> {
    val request = requestOf(createTempDirectory(COMPARE), versions)
    return request to Launcher.compare(request)
}

class ComparisonLaunchTest : FunSpec({
    context("a comparison with the baseline") {
        test("fails on a change that breaks the clients and writes it with its place in both reports") {
            val (request, verdict) = compared(Versions(baseline = openApi("get", "post"), candidate = openApi("post")))

            verdict.isAccepted shouldBe false
            verdict.failures.joinToString() shouldContain REMOVED
            request.report.readText() shouldContain """"outcome": "Compared""""
            request.sarif.readText() shouldContain REMOVED
            request.sarif.readText() shouldContain """"level": "warning""""
            request.commonKotlin.exists() shouldBe false
        }

        test("accepts a breaking change that is acknowledged with a reason") {
            val versions = Versions(
                baseline = openApi("get", "post"),
                candidate = openApi("post"),
                acknowledgements = mapOf(REMOVED to REASON),
            )

            compared(versions).second.isAccepted shouldBe true
        }

        test("notes a change that breaks nobody and passes") {
            val (request, verdict) = compared(Versions(baseline = openApi("post"), candidate = openApi("get", "post")))

            verdict.isAccepted shouldBe true
            verdict.lines.joinToString() shouldContain "note: [$ADDED]"
            request.sarif.readText() shouldContain """"level": "note""""
        }

        test("judges the side it is asked for: a removal breaks clients, an addition the providers") {
            val removed = Versions(baseline = openApi("get", "post"), candidate = openApi("post"), breaking = "Both")
            val served = Versions(
                baseline = openApi("get", "post"),
                candidate = openApi("post"),
                breaking = "Providers",
            )
            val added = Versions(baseline = openApi("post"), candidate = openApi("get", "post"), breaking = "Providers")

            compared(removed).second.isAccepted shouldBe false
            compared(served).second.isAccepted shouldBe true
            compared(added).second.isAccepted shouldBe false
        }

        test("compares AsyncAPI documents by their own rules") {
            val versions = Versions(
                baseline = asyncApi("room", "lobby"),
                candidate = asyncApi("room"),
                kind = LaunchKind.AsyncApi,
            )

            compared(versions).second.failures.joinToString() shouldContain "asyncapi.compatibility.channel-removed"
        }

        test("is refused with the findings of a version that cannot be read") {
            val (_, verdict) = compared(Versions(baseline = "openapi: 3.1.0\n", candidate = openApi("post")))

            verdict.isAccepted shouldBe false
            verdict.failures.joinToString() shouldContain "petstore-1.0.0.yaml"
        }
    }

    context("a comparison that cannot start") {
        test("fails for a baseline outside the root, an unknown side or no baseline at all") {
            val outside = Versions(baseline = openApi("get"), candidate = openApi("get"), isBaselineInside = false)
            val side = Versions(baseline = openApi("get"), candidate = openApi("get"), breaking = "Everyone")
            val missing = requestOf(createTempDirectory(COMPARE), Versions(openApi("get"), openApi("get")))
            val bare = LaunchRequest(
                specification = missing.specification,
                root = missing.root,
                overlays = emptyList(),
                commonKotlin = missing.commonKotlin,
                jvmKotlin = missing.jvmKotlin,
                resources = missing.resources,
                report = missing.report,
                sarif = missing.sarif,
                packageName = missing.packageName,
                apiName = missing.apiName,
                typePrefix = "",
                artifacts = missing.artifacts,
                visibility = missing.visibility,
                generatorVersion = missing.generatorVersion,
                acknowledgements = emptyMap(),
            )

            compared(outside).second.failures.joinToString() shouldContain "lies outside its root"
            compared(side).second.failures.joinToString() shouldContain "The side Everyone is unknown"
            Launcher.compare(bare).failures.joinToString() shouldContain "needs the baseline"
        }
    }
})
