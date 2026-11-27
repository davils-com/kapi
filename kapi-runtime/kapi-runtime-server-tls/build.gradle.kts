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
    id("kapi.jvm-library")
}

kreate {
    project {
        description = "The bridge of the kapi server to mutual TLS of krypt: the client certificate chain that " +
            "krypt-tls verified for a connection becomes the verified certificates of the peer of a request, for " +
            "the JVM."
    }
}

dependencies {
    api(project(":kapi-runtime:kapi-runtime-server"))
    api(libs.krypt.tls)
}

configurations.matching { configuration -> configuration.name == "unitTestImplementation" }.configureEach {
    dependencies.add(project.dependencies.create(libs.ktor.server.test.host.get()))
    dependencies.add(project.dependencies.create(libs.krypt.authority.get()))
}
