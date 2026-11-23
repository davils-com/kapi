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
import org.gradle.api.provider.Provider

private const val EXTENSION: String = "kapi"

private const val GENERATOR_SCOPE: String = "kapiCodegen"

private const val GENERATOR_CLASSPATH: String = "kapiCodegenClasspath"

private const val UNSPECIFIED: String = "unspecified"

/**
 * The plugin `com.davils.kapi`: generates Kotlin from the API documents of the `kapi { }` block, lints them and
 * reports every diagnostic as JSON and SARIF; see [CodegenExtension] for the tasks, the source sets and the
 * runtimes it wires.
 *
 * @since 1.0.0
 */
public class CodegenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION, CodegenExtension::class.java)
        extension.version.convention(pluginVersion())
        extension.runtimes.enabled.convention(true)
        val wiring = DocumentWiring(project, extension, generatorClasspath(project, extension))
        wiring.lifecycle()
        extension.openApi.all(wiring::wire)
        extension.asyncApi.all(wiring::wire)
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
                val usage = objects.named(Usage::class.java, Usage.JAVA_RUNTIME)
                val category = objects.named(Category::class.java, Category.LIBRARY)
                val jar = objects.named(LibraryElements::class.java, LibraryElements.JAR)
                val bundling = objects.named(Bundling::class.java, Bundling.EXTERNAL)
                attributes.attribute(Usage.USAGE_ATTRIBUTE, usage)
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, category)
                attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, jar)
                attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, bundling)
            }
        }
    }

    private fun pluginVersion(): String {
        val found = CodegenPlugin::class.java.`package`
        val versions = listOfNotNull(found.implementationVersion, UNSPECIFIED)
        return versions.first()
    }
}
