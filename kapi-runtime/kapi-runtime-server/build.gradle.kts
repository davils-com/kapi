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
    id("kapi.jvm-serialization-library")
}

kreate {
    project {
        description = "The server runtime of kapi, the API toolkit of Davils: a Ktor server plugin that mounts " +
            "generated services, reads and checks requests as a whole, negotiates content, streams, authorizes " +
            "callers, answers with problem details and serves WebSocket channels, for the JVM."
    }
}

dependencies {
    api(project(":kapi-runtime:kapi-runtime-core"))
    api(libs.ktor.server.core)
    api(libs.ktor.server.websockets)
    api(libs.ktor.server.sse)
    api(libs.kollect.core)
    api(libs.konfetti.core)
    api(libs.krypt.jose)
    api(libs.krypt.x509)
    api(libs.kontinuity.cache)
    api(libs.kontinuity.rate.limiter)
    implementation(libs.kontinuity.bulkhead)
}

configurations.matching { configuration -> configuration.name == "unitTestImplementation" }.configureEach {
    dependencies.add(project.dependencies.create(libs.ktor.server.test.host.get()))
    dependencies.add(project.dependencies.create(libs.ktor.server.cio.get()))
    dependencies.add(project.dependencies.create(libs.ktor.client.cio.get()))
}
