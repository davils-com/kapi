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
        description = "Test support of kapi, the API toolkit of Davils: fake credentials, mock engines and contract " +
            "assertions for code generated from OpenAPI and AsyncAPI documents, for the JVM and WebAssembly; never " +
            "on a production class path."
    }
}

dependencies {
    commonMainApi(project(":kapi-runtime:kapi-runtime-client"))
    commonMainApi(project(":kapi-spec:kapi-spec-openapi"))
    commonMainApi(project(":kapi-spec:kapi-spec-schema"))
    commonMainApi(libs.ktor.client.mock)
    jvmMainApi(project(":kapi-runtime:kapi-runtime-server"))
}
