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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaToolchainService
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

private const val EXTENSION: String = "kapi"

private const val GENERATOR_SCOPE: String = "kapiCodegen"

private const val GENERATOR_CLASSPATH: String = "kapiCodegenClasspath"

private const val MULTIPLATFORM: String = "org.jetbrains.kotlin.multiplatform"

private const val KOTLIN_JVM: String = "org.jetbrains.kotlin.jvm"

private const val UNSPECIFIED: String = "unspecified"

/**
 * The plugin `com.davils.kapi`: generates Kotlin from the API documents of the `kapi { }` block.
 *
 * For every specification it registers `kapiGenerate<Name>`, adds its output to the sources of the project and adds
 * the runtimes the chosen artifacts need: in a multiplatform project the models and clients go to `commonMain` and the
 * servers and mocks to `jvmMain`; in a JVM project everything goes to `main`. The generator runs in a worker process
 * of its own, on a class path that never meets the build's.
 *
 * @since 1.0.0
 */
public class CodegenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION, CodegenExtension::class.java)
        extension.version.convention(pluginVersion())
        val classpath = generatorClasspath(project, extension)
        extension.openApi.all { spec -> configure(project, extension, spec, classpath) }
    }

    private fun generatorClasspath(project: Project, extension: CodegenExtension): Provider<out Configuration> {
        val scope = project.configurations.dependencyScope(GENERATOR_SCOPE) { configuration ->
            val generator = extension.version.map { version -> Runtimes.notation(Runtimes.GENERATOR, version) }
            configuration.dependencies.addLater(generator.map { notation -> project.dependencies.create(notation) })
        }
        return project.configurations.resolvable(GENERATOR_CLASSPATH) { configuration ->
            configuration.extendsFrom(scope.get())
            configuration.attributes { attributes ->
                val objects = project.objects
                attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
                val jar = objects.named(LibraryElements::class.java, LibraryElements.JAR)
                attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, jar)
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
            }
        }
    }

    private fun configure(
        project: Project,
        extension: CodegenExtension,
        spec: SpecificationSpec,
        classpath: Provider<out Configuration>,
    ) {
        conventions(project, spec)
        val task = register(project, extension, spec, classpath)
        project.pluginManager.withPlugin(MULTIPLATFORM) { multiplatform(project, extension, spec, task) }
        project.pluginManager.withPlugin(KOTLIN_JVM) { jvm(project, extension, spec, task) }
    }

    private fun conventions(project: Project, spec: SpecificationSpec) {
        val folder = project.layout.projectDirectory.dir("src/main/openapi")
        spec.source.convention(folder.file("${spec.name}.yaml"))
        spec.root.convention(spec.source.map { source -> project.layout.projectDirectory.dir(source.asFile.parent) })
        spec.apiName.convention(upperCamel(spec.name))
    }

    private fun register(
        project: Project,
        extension: CodegenExtension,
        spec: SpecificationSpec,
        classpath: Provider<out Configuration>,
    ): TaskProvider<Generate> {
        val output = project.layout.buildDirectory.dir("generated/kapi/${spec.name}")
        val reports = project.layout.buildDirectory.dir("reports/kapi/${spec.name}")
        return project.tasks.register("kapiGenerate${upperCamel(spec.name)}", Generate::class.java) { task ->
            task.specification.set(spec.source)
            task.root.set(spec.root)
            task.sources.from(spec.root)
            task.generatorClasspath.from(classpath)
            task.overlays.set(spec.overlays)
            task.packageName.set(spec.packageName)
            task.apiName.set(spec.apiName)
            task.typePrefix.set(spec.typePrefix)
            task.artifacts.set(project.provider { spec.artifacts.names() })
            task.visibility.set(spec.visibility.map(Visibility::name))
            task.generatorVersion.set(extension.version)
            task.acknowledgements.set(spec.acknowledgements)
            task.commonKotlin.set(output.map { folder -> folder.dir("common/kotlin") })
            task.jvmKotlin.set(output.map { folder -> folder.dir("jvm/kotlin") })
            task.resources.set(output.map { folder -> folder.dir("resources") })
            task.report.set(reports.map { folder -> folder.file("report.json") })
            task.sarif.set(reports.map { folder -> folder.file("report.sarif") })
            task.launcher.convention(launcherOf(project))
        }
    }

    private fun launcherOf(project: Project): Provider<org.gradle.jvm.toolchain.JavaLauncher> {
        val java = project.extensions.findByType(JavaPluginExtension::class.java)
        val toolchains = project.extensions.findByType(JavaToolchainService::class.java)
        if (java == null || toolchains == null) return project.provider { null }
        return toolchains.launcherFor(java.toolchain)
    }

    private fun multiplatform(
        project: Project,
        extension: CodegenExtension,
        spec: SpecificationSpec,
        task: TaskProvider<Generate>,
    ) {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.sourceSets.named("commonMain") { set ->
            set.kotlin.srcDir(task.flatMap(Generate::commonKotlin))
            set.resources.srcDir(task.flatMap(Generate::resources))
        }
        kotlin.sourceSets.matching { set -> set.name == "jvmMain" }.configureEach { set ->
            set.kotlin.srcDir(task.flatMap(Generate::jvmKotlin))
        }
        runtimes(project, "commonMainApi", extension, spec) { artifacts -> Runtimes.commonOf(artifacts) }
        runtimes(project, "jvmMainApi", extension, spec) { artifacts -> Runtimes.jvmOf(artifacts) }
    }

    private fun jvm(
        project: Project,
        extension: CodegenExtension,
        spec: SpecificationSpec,
        task: TaskProvider<Generate>,
    ) {
        val kotlin = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        kotlin.sourceSets.named("main") { set ->
            set.kotlin.srcDir(task.flatMap(Generate::commonKotlin))
            set.kotlin.srcDir(task.flatMap(Generate::jvmKotlin))
            set.resources.srcDir(task.flatMap(Generate::resources))
        }
        val configuration = "api".takeIf { project.configurations.names.contains("api") } ?: "implementation"
        runtimes(project, configuration, extension, spec) { artifacts ->
            Runtimes.commonOf(artifacts) + Runtimes.jvmOf(artifacts)
        }
    }

    private fun runtimes(
        project: Project,
        configuration: String,
        extension: CodegenExtension,
        spec: SpecificationSpec,
        modules: (Set<String>) -> List<String>,
    ) {
        project.configurations.matching { candidate -> candidate.name == configuration }.configureEach { found ->
            val notations = project.provider {
                val version = extension.version.get()
                modules(spec.artifacts.names()).map { module -> Runtimes.notation(module, version) }
            }
            found.dependencies.addAllLater(
                notations.map { list -> list.map { notation -> project.dependencies.create(notation) } },
            )
        }
    }

    private fun upperCamel(name: String): String = name.split('-', '_', '.', ' ')
        .filter(String::isNotEmpty)
        .joinToString("") { part -> part.replaceFirstChar(Char::uppercaseChar) }

    private fun pluginVersion(): String = CodegenPlugin::class.java.`package`.implementationVersion ?: UNSPECIFIED

}
