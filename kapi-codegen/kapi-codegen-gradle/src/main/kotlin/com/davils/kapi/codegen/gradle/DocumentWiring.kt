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

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.language.base.plugins.LifecycleBasePlugin

private const val LINT: String = "kapiLint"

private const val CHECK: String = "kapiCheck"

private const val GENERATE: String = "kapiGenerate"

private const val BASE: String = "base"

internal class DocumentWiring(
    private val project: Project,
    private val extension: CodegenExtension,
    private val resolved: Provider<out Configuration>,
) {
    private val sources: SourceWiring = SourceWiring(project, extension)

    fun lifecycle() {
        val lint = project.tasks.register(LINT) { task ->
            task.group = DocumentTask.GROUP
            task.description = "Lints every API document of kapi."
            task.dependsOn(project.tasks.withType(Lint::class.java))
        }
        val check = project.tasks.register(CHECK) { task ->
            task.group = DocumentTask.GROUP
            task.description = "Runs every check of kapi."
            task.dependsOn(lint)
        }
        project.pluginManager.withPlugin(BASE) {
            project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { task -> task.dependsOn(check) }
        }
    }

    fun wire(spec: DocumentSpec) {
        val suffix = upperCamel(spec.name)
        val names = listOf(GENERATE + suffix, LINT + suffix)
        val taken = names.filter { name -> name in project.tasks.names }
        if (taken.isNotEmpty()) throw clash(spec, taken)
        conventions(spec)
        val generate = project.tasks.register(GENERATE + suffix, Generate::class.java) { task ->
            val output = project.layout.buildDirectory.dir("generated/kapi/${spec.name}")
            configure(task, spec)
            task.commonKotlin.set(output.map { folder -> folder.dir("common/kotlin") })
            task.jvmKotlin.set(output.map { folder -> folder.dir("jvm/kotlin") })
            task.resources.set(output.map { folder -> folder.dir("resources") })
            task.report.set(reportsOf(spec).map { folder -> folder.file("report.json") })
            task.sarif.set(reportsOf(spec).map { folder -> folder.file("report.sarif") })
        }
        project.tasks.register(LINT + suffix, Lint::class.java) { task ->
            configure(task, spec)
            task.report.set(reportsOf(spec).map { folder -> folder.file("lint.json") })
            task.sarif.set(reportsOf(spec).map { folder -> folder.file("lint.sarif") })
        }
        sources.wire(spec, generate)
    }

    private fun clash(spec: DocumentSpec, taken: List<String>): InvalidUserDataException = InvalidUserDataException(
        "The API document ${spec.name} needs the tasks ${taken.joinToString()}, which another document or kapi " +
            "already takes; give every document of openApi and asyncApi a name of its own",
    )

    private fun conventions(spec: DocumentSpec) {
        val folder = project.layout.projectDirectory.dir("src/main/${spec.kind().folder}")
        spec.source.convention(folder.file("${spec.name}.yaml"))
        spec.root.convention(spec.source.map { source -> project.layout.projectDirectory.dir(source.asFile.parent) })
        spec.apiName.convention(upperCamel(spec.name))
    }

    private fun configure(task: DocumentTask, spec: DocumentSpec) {
        task.specification.set(spec.source)
        task.root.set(spec.root)
        task.sources.from(spec.root)
        task.generatorClasspath.from(generatorClasspath())
        task.kind.set(spec.kind().name)
        task.overlays.set(project.provider { spec.overlayPaths() })
        task.packageName.set(spec.packageName)
        task.apiName.set(spec.apiName)
        task.typePrefix.set(spec.typePrefix)
        task.artifacts.set(project.provider { spec.artifactNames() })
        task.visibility.set(spec.visibility.map(Visibility::name))
        task.generatorVersion.set(extension.version)
        task.acknowledgements.set(spec.acknowledgements)
        task.launcher.convention(launcher())
    }

    private fun generatorClasspath(): FileCollection {
        val chosen = extension.generatorClasspath
        return chosen.takeIf { classpath -> classpath.from.isNotEmpty() } ?: resolved.get()
    }

    private fun reportsOf(spec: DocumentSpec): Provider<Directory> =
        project.layout.buildDirectory.dir("reports/kapi/${spec.name}")

    private fun launcher(): Provider<JavaLauncher> {
        val java = project.extensions.findByType(JavaPluginExtension::class.java) ?: return project.provider { null }
        val toolchains = project.extensions.getByType(JavaToolchainService::class.java)
        return toolchains.launcherFor(java.toolchain)
    }

    companion object {
        fun upperCamel(name: String): String {
            val parts = name.split('-', '_', '.', ' ')
            return parts.joinToString("") { part -> part.take(1).uppercase() + part.drop(1) }
        }
    }
}
