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


plugins {
    id("kapi.gradle-plugin")
}

kreate {
    project {
        description = "The Gradle plugin of kapi, the API toolkit of Davils: generates Kotlin from OpenAPI and " +
            "AsyncAPI documents in an isolated worker, wires the sources and runtimes into JVM and multiplatform " +
            "projects and reports every diagnostic as JSON and SARIF."
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
}
