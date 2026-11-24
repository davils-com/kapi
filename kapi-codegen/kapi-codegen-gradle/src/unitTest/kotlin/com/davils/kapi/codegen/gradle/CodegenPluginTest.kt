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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.types.shouldBeInstanceOf
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

private const val PACKAGE: String = "com.example"

private const val VERSION: String = "1.2.3"

private const val CORE: String = "com.davils:kapi-runtime-core:$VERSION"

private const val SERVER: String = "com.davils:kapi-runtime-server:$VERSION"

private const val PET_STORE: String = "pet-store"

private const val GENERATE_A: String = "kapiGenerateA"

private const val CLIENT: String = "com.davils:kapi-runtime-client:$VERSION"

private const val MODELS: String = "Models"

private const val REASON: String = "The vendor publishes Swagger only"

private const val ACKNOWLEDGED: String = "openapi.version.upgraded"

private fun project(vararg plugins: String): Project {
    val project = ProjectBuilder.builder().build()
    plugins.forEach(project.pluginManager::apply)
    project.pluginManager.apply("com.davils.kapi")
    return project
}

private fun Project.kapi(): CodegenExtension = extensions.getByType(CodegenExtension::class.java)

private fun Project.document(name: String) {
    kapi().openApi(name) { spec -> spec.packageName.set(PACKAGE) }
}

private inline fun <reified T : Task> Project.task(name: String): T = tasks.getByName(name) as T

private fun Project.notationsOf(configuration: String): List<String> {
    val dependencies = configurations.getByName(configuration).dependencies
    return dependencies.map { dependency ->
        "${dependency.group.orEmpty()}:${dependency.name}:${dependency.version.orEmpty()}"
    }
}

private fun Task.dependencyNames(): Set<String> {
    val dependencies = taskDependencies.getDependencies(this)
    return dependencies.map(Task::getName).toSet()
}

private fun Provider<out FileSystemLocation>.path(): String {
    val file = get().asFile
    return file.invariantSeparatorsPath
}

private fun KotlinSourceSet.folders(): List<String> = kotlin.srcDirs.map { folder -> folder.invariantSeparatorsPath }

class CodegenPluginTest : FunSpec({
    context("an OpenAPI document") {
        test("registers its generate and lint tasks in the group of kapi") {
            val project = project()

            project.kapi().openApi(PET_STORE) { spec -> spec.packageName.set("com.example.petstore") }

            val generate = project.task<Generate>("kapiGeneratePetStore")
            val lint = project.task<Lint>("kapiLintPetStore")
            generate.group shouldBe "kapi"
            generate.packageName.get() shouldBe "com.example.petstore"
            generate.kind.get() shouldBe "OpenApi"
            generate.report.path() shouldEndWith "reports/kapi/pet-store/report.json"
            lint.report.path() shouldEndWith "reports/kapi/pet-store/lint.json"
            lint.sarif.path() shouldEndWith "reports/kapi/pet-store/lint.sarif"
        }

        test("reads src/main/openapi/<name>.yaml and is named after the document by default") {
            val project = project()

            project.document(PET_STORE)

            val spec = project.kapi().openApi.getByName(PET_STORE)
            spec.source.path() shouldEndWith "src/main/openapi/pet-store.yaml"
            spec.root.path() shouldEndWith "src/main/openapi"
            spec.apiName.get() shouldBe "PetStore"
            spec.typePrefix.get() shouldBe ""
            spec.visibility.get() shouldBe Visibility.Public
        }

        test("generates models only unless more is chosen") {
            val project = project()

            project.kapi().openApi("a") { spec ->
                spec.packageName.set(PACKAGE)
                spec.artifacts { artifacts -> artifacts.client.set(true) }
            }

            project.task<Generate>(GENERATE_A).artifacts.get() shouldBe setOf(MODELS, "Client")
        }

        test("passes overlays and acknowledgements on in order") {
            val project = project()

            project.kapi().openApi("a") { spec ->
                spec.packageName.set(PACKAGE)
                spec.overlay("one.yaml")
                spec.overlay("two.yaml")
                spec.acknowledge(code = ACKNOWLEDGED, reason = REASON)
            }

            val task = project.task<Generate>(GENERATE_A)
            task.overlays.get() shouldContainExactly listOf("one.yaml", "two.yaml")
            task.acknowledgements.get() shouldBe mapOf(ACKNOWLEDGED to REASON)
        }
    }

    context("an AsyncAPI document") {
        test("reads src/main/asyncapi/<name>.yaml, takes no overlays and chooses models, client and server") {
            val project = project()

            project.kapi().asyncApi("chat") { spec ->
                spec.packageName.set(PACKAGE)
                spec.artifacts { artifacts -> artifacts.server.set(true) }
            }

            val spec = project.kapi().asyncApi.getByName("chat")
            val task = project.task<Generate>("kapiGenerateChat")
            spec.source.path() shouldEndWith "src/main/asyncapi/chat.yaml"
            task.kind.get() shouldBe "AsyncApi"
            task.overlays.get().shouldBeEmpty()
            task.artifacts.get() shouldBe setOf(MODELS, "Server")
        }

        test("refuses a name that an OpenAPI document already takes") {
            val project = project()
            project.document("chat")

            val failure = shouldThrow<InvalidUserCodeException> {
                project.kapi().asyncApi("chat") { spec -> spec.packageName.set(PACKAGE) }
            }

            val cause = failure.cause.shouldBeInstanceOf<InvalidUserDataException>()
            cause.message.orEmpty() shouldContain "kapiGenerateChat, kapiLintChat"
        }
    }

    context("the lifecycle") {
        test("kapiLint and kapiCompatibility run theirs, kapiCheck runs both and check runs kapiCheck") {
            val project = project("base")

            project.document("a")
            project.kapi().asyncApi("b") { spec -> spec.packageName.set(PACKAGE) }

            project.task<Task>("kapiLint").dependencyNames() shouldBe setOf("kapiLintA", "kapiLintB")
            project.task<Task>("kapiCompatibility").dependencyNames() shouldBe
                setOf("kapiCompatibilityA", "kapiCompatibilityB")
            project.task<Task>("kapiCheck").dependencyNames() shouldBe setOf("kapiLint", "kapiCompatibility")
            project.task<Task>("check").dependencyNames() shouldContain "kapiCheck"
        }
    }

    context("the generator") {
        test("is resolved from the repositories at the version of kapi unless the build names its class path") {
            val project = project()
            project.kapi().version.set(VERSION)
            project.document("a")
            project.document("b")

            val resolved = project.task<Generate>(GENERATE_A)
            project.kapi().generatorClasspath.from("generator.jar")
            val chosen = project.task<Lint>("kapiLintB")

            project.notationsOf("kapiCodegen") shouldContainExactly listOf("com.davils:kapi-codegen-core:$VERSION")
            val source = resolved.generatorClasspath.from.single()
            source.toString() shouldContain "kapiCodegenClasspath"
            chosen.generatorClasspath.files.map { file -> file.name } shouldContainExactly listOf("generator.jar")
        }

        test("runs on the toolchain of a Java project and on the JVM of Gradle otherwise") {
            val java = project("java")
            val bare = project()

            java.document("a")
            bare.document("a")

            java.task<Generate>(GENERATE_A).launcher.isPresent shouldBe true
            bare.task<Generate>(GENERATE_A).launcher.isPresent shouldBe false
        }
    }

    context("a JVM project") {
        test("compiles every generated folder in main and gets the runtimes of the chosen artifacts") {
            val project = project("org.jetbrains.kotlin.jvm")
            project.kapi().version.set(VERSION)

            project.kapi().openApi("a") { spec ->
                spec.packageName.set(PACKAGE)
                spec.artifacts { artifacts -> artifacts.server.set(true) }
            }

            val kotlin = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
            val folders = kotlin.sourceSets.getByName("main").folders()
            folders.any { folder -> folder.endsWith("generated/kapi/a/common/kotlin") } shouldBe true
            folders.any { folder -> folder.endsWith("generated/kapi/a/jvm/kotlin") } shouldBe true
            project.notationsOf("api") shouldContainExactly listOf(CORE, SERVER)
        }

        test("adds no runtime when the build declares them itself") {
            val project = project("org.jetbrains.kotlin.jvm")

            project.kapi().runtimes { runtimes -> runtimes.enabled.set(false) }
            project.document("a")

            project.notationsOf("api").shouldBeEmpty()
        }
    }

    context("a multiplatform project") {
        test("compiles models and clients in commonMain and servers in jvmMain with their runtimes") {
            val project = project("org.jetbrains.kotlin.multiplatform")
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            kotlin.jvm()
            project.kapi().version.set(VERSION)

            project.kapi().asyncApi("chat") { spec ->
                spec.packageName.set(PACKAGE)
                spec.artifacts { artifacts ->
                    artifacts.client.set(true)
                    artifacts.server.set(true)
                }
            }

            val common = kotlin.sourceSets.getByName("commonMain").folders()
            val jvm = kotlin.sourceSets.getByName("jvmMain").folders()
            common.any { folder -> folder.endsWith("generated/kapi/chat/common/kotlin") } shouldBe true
            jvm.any { folder -> folder.endsWith("generated/kapi/chat/jvm/kotlin") } shouldBe true
            project.notationsOf("commonMainApi") shouldContainAll listOf(CORE, CLIENT)
            project.notationsOf("jvmMainApi") shouldContainExactly listOf(SERVER)
        }
    }

    context("the runtimes") {
        test("are the core and the client for a client, and the server for a server") {
            val common = Runtimes.commonOf(setOf(MODELS, "Client"))

            common shouldContainExactly listOf("kapi-runtime-core", "kapi-runtime-client")
            Runtimes.jvmOf(setOf("Server", "Mocks")) shouldContainExactly listOf("kapi-runtime-server")
            val notation = Runtimes.notation(module = "kapi-runtime-core", version = "1.0.0")
            notation shouldBe "com.davils:kapi-runtime-core:1.0.0"
        }

        test("follow the version of the plugin by default") {
            val extension = project().kapi()
            extension.version.get() shouldBe "unspecified"
        }
    }
})
