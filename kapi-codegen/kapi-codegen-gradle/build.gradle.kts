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

import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute

plugins {
    id("kapi.gradle-plugin")
}

kreate {
    project {
        description = "The Gradle plugin of kapi, the API toolkit of Davils: generates Kotlin from OpenAPI and " +
            "AsyncAPI documents in an isolated worker, wires the sources and runtimes into JVM and multiplatform " +
            "projects, lints the documents and reports every diagnostic as JSON and SARIF."

        coverage {
            instrumentation {
                includedClasses.add("com.davils.kapi.codegen.gradle.*")
            }
        }
    }
}

gradlePlugin {
    website = "https://github.com/davils-com/kapi"
    vcsUrl = "https://github.com/davils-com/kapi"
    plugins {
        register("kapi") {
            id = "com.davils.kapi"
            displayName = "kapi"
            description = "Generates typed Kotlin clients, servers and models on Ktor from OpenAPI and AsyncAPI."
            implementationClass = "com.davils.kapi.codegen.gradle.CodegenPlugin"
            tags = listOf("openapi", "asyncapi", "codegen", "ktor", "kotlin", "multiplatform", "davils")
        }
    }
}

dependencies {
    compileOnly(project(":kapi-codegen:kapi-codegen-core"))
    compileOnly(libs.kotlin.gradle.plugin)
}

configurations.matching { configuration -> configuration.name == "unitTestImplementation" }.configureEach {
    dependencies.add(project.dependencies.gradleApi())
    dependencies.add(project.dependencies.gradleTestKit())
    dependencies.add(project.dependencies.create(libs.kotlin.gradle.plugin.get()))
    dependencies.add(project.dependencies.project(":kapi-codegen:kapi-codegen-core"))
}

val generator: String = ":kapi-codegen:kapi-codegen-core"
val client: String = ":kapi-runtime:kapi-runtime-client"
val server: String = ":kapi-runtime:kapi-runtime-server"

fun classpathOf(name: String, notations: List<Any>, wasm: Boolean): NamedDomainObjectProvider<ResolvableConfiguration> {
    val scope = configurations.dependencyScope("${name}Dependencies") {
        notations.forEach { notation -> dependencies.add(project.dependencies.create(notation)) }
    }
    return configurations.resolvable(name) {
        extendsFrom(scope.get())
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            if (wasm) {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named("kotlin-runtime"))
                attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named("non-jvm"))
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
                attribute(KotlinWasmTargetAttribute.wasmTargetAttribute, KotlinWasmTargetAttribute.js)
                return@attributes
            }
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named("standard-jvm"))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, objects.named(gradle.gradleVersion))
        }
    }
}

val pluginsUnderTest = classpathOf(
    name = "functionalTestPlugins",
    notations = listOf(libs.kotlin.gradle.plugin.get(), libs.kotlin.serialization.gradle.plugin.get()),
    wasm = false,
)
val generatorUnderTest = classpathOf(name = "functionalTestGenerator", notations = listOf(project(generator)), wasm = false)
val jvmRuntimes = classpathOf(name = "functionalTestJvm", notations = listOf(project(client), project(server)), wasm = false)
val wasmRuntimes = classpathOf(name = "functionalTestWasm", notations = listOf(project(client)), wasm = true)

tasks.withType<Test>().matching { task -> task.name == "unitTest" }.configureEach {
    val main = sourceSets.main.get()
    val classpaths = mapOf(
        "kapi.test.plugin" to files(main.runtimeClasspath, pluginsUnderTest),
        "kapi.test.generator" to files(generatorUnderTest),
        "kapi.test.jvm" to files(jvmRuntimes),
        "kapi.test.wasm" to files(wasmRuntimes),
    )
    classpaths.forEach { (name, classpath) -> inputs.files(classpath).withPropertyName(name).withNormalizer(ClasspathNormalizer::class) }
    jvmArgumentProviders.add(CommandLineArgumentProvider { classpaths.map { (name, classpath) -> "-D$name=${classpath.asPath}" } })
    maxHeapSize = "3g"
}
