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
        description = "The code generator of kapi, the API toolkit of Davils: audits an OpenAPI or AsyncAPI " +
            "document, names what it declares and generates Kotlin models, clients, servers, mocks, webhooks and " +
            "WebSocket channels with KotlinPoet, with the provenance of every file, for the JVM."
    }
}

dependencies {
    api(project(":kapi-spec:kapi-spec-openapi"))
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinx.serialization.json)
}
