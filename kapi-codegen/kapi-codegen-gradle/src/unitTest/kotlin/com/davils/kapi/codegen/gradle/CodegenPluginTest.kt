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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

private fun project(): Project {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply("com.davils.kapi")
    return project
}

private fun Project.kapi(): CodegenExtension = extensions.getByType(CodegenExtension::class.java)

class CodegenPluginTest : FunSpec({
    context("a specification") {
        test("registers its generate task in the group of kapi") {
            val project = project()

            project.kapi().openApi("pet-store") { spec -> spec.packageName.set("com.example.petstore") }

            val task = project.tasks.getByName("kapiGeneratePetStore") as Generate
            task.group shouldBe "kapi"
            task.packageName.get() shouldBe "com.example.petstore"
        }

        test("reads src/main/openapi/<name>.yaml and is named after the specification by default") {
            val project = project()

            project.kapi().openApi("pet-store") { spec -> spec.packageName.set("com.example") }

            val spec = project.kapi().openApi.getByName("pet-store")
            spec.source.get().asFile.path shouldEndWith "src/main/openapi/pet-store.yaml"
            spec.apiName.get() shouldBe "PetStore"
            spec.typePrefix.get() shouldBe ""
            spec.visibility.get() shouldBe Visibility.Public
        }

        test("generates models only unless more is chosen") {
            val project = project()

            project.kapi().openApi("a") { spec ->
                spec.packageName.set("com.example")
                spec.artifacts { artifacts -> artifacts.client.set(true) }
            }

            project.kapi().openApi.getByName("a").artifacts.names() shouldBe setOf("Models", "Client")
        }

        test("passes overlays and acknowledgements on in order") {
            val project = project()

            project.kapi().openApi("a") { spec ->
                spec.packageName.set("com.example")
                spec.overlay("one.yaml")
                spec.overlay("two.yaml")
                spec.acknowledge("openapi.version.upgraded", "The vendor publishes Swagger only")
            }

            val task = project.tasks.getByName("kapiGenerateA") as Generate
            task.overlays.get() shouldContainExactly listOf("one.yaml", "two.yaml")
            task.acknowledgements.get() shouldBe mapOf("openapi.version.upgraded" to "The vendor publishes Swagger only")
        }
    }

    context("the runtimes") {
        test("are the core and the client for a client, and the server for a server") {
            Runtimes.commonOf(setOf("Models", "Client")) shouldContainExactly listOf("kapi-runtime-core", "kapi-runtime-client")
            Runtimes.jvmOf(setOf("Server", "Mocks")) shouldContainExactly listOf("kapi-runtime-server")
            Runtimes.notation("kapi-runtime-core", "1.0.0") shouldBe "com.davils:kapi-runtime-core:1.0.0"
        }
    }
})
