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

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val MULTIPLATFORM: String = "org.jetbrains.kotlin.multiplatform"

private const val KOTLIN_JVM: String = "org.jetbrains.kotlin.jvm"

private const val API: String = "api"

internal class SourceWiring(private val project: Project, private val extension: CodegenExtension) {
    fun wire(spec: DocumentSpec, task: TaskProvider<Generate>) {
        project.pluginManager.withPlugin(MULTIPLATFORM) { multiplatform(spec, task) }
        project.pluginManager.withPlugin(KOTLIN_JVM) { jvm(spec, task) }
    }

    private fun multiplatform(spec: DocumentSpec, task: TaskProvider<Generate>) {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.sourceSets.named("commonMain") { set ->
            set.kotlin.srcDir(task.flatMap(Generate::commonKotlin))
            set.resources.srcDir(task.flatMap(Generate::resources))
        }
        val jvm = kotlin.sourceSets.matching { set -> set.name == "jvmMain" }
        jvm.configureEach { set -> set.kotlin.srcDir(task.flatMap(Generate::jvmKotlin)) }
        runtimes("commonMainApi", spec) { artifacts -> Runtimes.commonOf(artifacts) }
        runtimes("jvmMainApi", spec) { artifacts -> Runtimes.jvmOf(artifacts) }
    }

    private fun jvm(spec: DocumentSpec, task: TaskProvider<Generate>) {
        val kotlin = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        kotlin.sourceSets.named("main") { set ->
            set.kotlin.srcDir(task.flatMap(Generate::commonKotlin))
            set.kotlin.srcDir(task.flatMap(Generate::jvmKotlin))
            set.resources.srcDir(task.flatMap(Generate::resources))
        }
        runtimes(API, spec) { artifacts -> Runtimes.commonOf(artifacts) + Runtimes.jvmOf(artifacts) }
    }

    private fun runtimes(configuration: String, spec: DocumentSpec, modules: (Set<String>) -> List<String>) {
        val matching = project.configurations.matching { candidate -> candidate.name == configuration }
        matching.configureEach { found ->
            val notations = project.provider {
                val isEnabled = extension.runtimes.enabled.get()
                val version = extension.version.get()
                val chosen = modules(spec.artifactNames()).filter { isEnabled }
                chosen.map { module -> project.dependencies.create(Runtimes.notation(module, version)) }
            }
            found.dependencies.addAllLater(notations)
        }
    }
}
