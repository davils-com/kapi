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
    id("kapi.serialization-library")
}

kreate {
    project {
        description = "The client runtime of kapi, the API toolkit of Davils: calls of generated clients over the " +
            "Ktor HttpClient of the application with credentials, OAuth 2, streams, server-sent events, pages, " +
            "resilience, telemetry and WebSocket sessions, for the JVM and WebAssembly."
    }
}

dependencies {
    commonMainApi(project(":kapi-runtime:kapi-runtime-core"))
    commonMainApi(libs.ktor.client.core)
    commonMainApi(libs.kontinuity.policy)
    commonMainApi(libs.kontinuity.circuit.breaker)
    commonMainApi(libs.kollect.core)
    commonMainApi(libs.konfetti.core)
    commonMainApi(libs.krypt.jose)
}

val testConfigurations: Set<String> = setOf("commonUnitTestImplementation", "wasmJsTestImplementation")

configurations.matching { configuration -> configuration.name in testConfigurations }.configureEach {
    dependencies.add(project.dependencies.create(libs.ktor.client.mock.get()))
}

configurations.matching { configuration -> configuration.name == "jvmUnitTestImplementation" }.configureEach {
    dependencies.add(project.dependencies.create(libs.ktor.server.cio.get()))
    dependencies.add(project.dependencies.create(libs.ktor.server.websockets.get()))
    dependencies.add(project.dependencies.create(libs.ktor.client.cio.get()))
}
