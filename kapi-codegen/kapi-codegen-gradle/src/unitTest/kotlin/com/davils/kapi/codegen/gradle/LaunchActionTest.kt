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

package com.davils.kapi.codegen.gradle

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val REPORT: String = "report.json"

private const val ACTION: String = "kapi-action"

private const val COMMON: String = "common"

private fun parametersIn(base: Path, document: String, kind: DocumentKind, isLint: Boolean): LaunchParameters {
    val builder = ProjectBuilder.builder()
    val project = builder.withProjectDir(base.toFile()).build()
    val parameters = project.objects.newInstance(LaunchParameters::class.java)
    val folder = base.resolve(kind.folder).createDirectories()
    val specification = folder.resolve("api.yaml")
    specification.writeText(document)
    val source = parameters.document
    source.specification.set(specification.toFile())
    source.root.set(folder.toFile())
    source.overlays.set(emptyList())
    source.kind.set(kind.name)
    options(parameters.options)
    outputs(outputs = parameters.outputs, base = base)
    parameters.mode.set(DocumentTask.LINT.takeIf { isLint } ?: DocumentTask.GENERATE)
    return parameters
}

private fun options(options: OptionParameters) {
    options.packageName.set("com.example.api")
    options.apiName.set("Api")
    options.typePrefix.set("")
    options.artifacts.set(setOf("Models", "Client", "Server"))
    options.visibility.set(Visibility.Internal.name)
    options.generatorVersion.set("test")
    options.acknowledgements.set(emptyMap())
}

private fun outputs(outputs: OutputParameters, base: Path) {
    outputs.commonKotlin.set(base.resolve(COMMON).toFile())
    outputs.jvmKotlin.set(base.resolve("jvm").toFile())
    outputs.resources.set(base.resolve("resources").toFile())
    outputs.report.set(base.resolve(REPORT).toFile())
    outputs.sarif.set(base.resolve("report.sarif").toFile())
}

private fun actionOf(base: Path, document: String, kind: DocumentKind, isLint: Boolean): LaunchAction {
    val parameters = parametersIn(base = base, document = document, kind = kind, isLint = isLint)
    return object : LaunchAction() {
        override fun getParameters(): LaunchParameters = parameters
    }
}

private fun contains(folder: Path, name: String): Boolean = Files.walk(folder).use { paths ->
    paths.anyMatch { path -> path.endsWith(name) }
}

class LaunchActionTest : FunSpec({
    test("generates an OpenAPI document into its folders") {
        val base = createTempDirectory(ACTION)
        val document = fixture("petstore.yaml")
        val action = actionOf(base = base, document = document, kind = DocumentKind.OpenApi, isLint = false)

        action.execute()

        contains(folder = base.resolve(COMMON), name = "ApiPet.kt") shouldBe true
        base.resolve(REPORT).readText() shouldContain """"outcome": "Generated""""
    }

    test("lints an AsyncAPI document without writing code and fails on its unacknowledged warning") {
        val base = createTempDirectory(ACTION)
        val action = actionOf(base = base, document = fixture("chat.yaml"), kind = DocumentKind.AsyncApi, isLint = true)

        val failure = shouldThrow<GradleException> { action.execute() }

        failure.message.orEmpty() shouldContain "does not pass the lint"
        failure.message.orEmpty() shouldContain "codegen.asyncapi.protocol.unsupported"
        base.resolve(COMMON).exists() shouldBe false
        base.resolve("report.sarif").exists() shouldBe true
    }

    test("compares a document with its baseline and accepts a change that breaks nobody") {
        val base = createTempDirectory(ACTION)
        val document = fixture("petstore.yaml")
        val parameters = parametersIn(base = base, document = document, kind = DocumentKind.OpenApi, isLint = false)
        val baseline = base.resolve(DocumentKind.OpenApi.folder).resolve("api-1.0.0.yaml")
        baseline.writeText(document)
        parameters.mode.set(DocumentTask.COMPARE)
        parameters.document.baseline.set(baseline.toFile())
        parameters.document.breaking.set(CompatibilitySide.Both.name)
        val action = object : LaunchAction() {
            override fun getParameters(): LaunchParameters = parameters
        }

        action.execute()

        base.resolve(REPORT).readText() shouldContain """"outcome": "Compared""""
        base.resolve(COMMON).exists() shouldBe false
    }

    test("fails a generation of a broken document with the rendered diagnostic") {
        val base = createTempDirectory(ACTION)
        val action = actionOf(base = base, document = "openapi: [", kind = DocumentKind.OpenApi, isLint = false)

        val failure = shouldThrow<GradleException> { action.execute() }

        failure.message.orEmpty() shouldContain "does not generate"
        failure.message.orEmpty() shouldContain "api.yaml:1:"
    }
})
