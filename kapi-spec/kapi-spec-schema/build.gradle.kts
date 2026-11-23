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
    id("kapi.kotlin-library")
}

kreate {
    project {
        description = "JSON Schema of kapi, the API toolkit of Davils: a bounded validator of JSON Schema 2020-12, " +
            "draft-07 and draft-04 with the dialects of OpenAPI 3.0 and 3.1, a linear-time pattern engine and format " +
            "assertions, for the JVM and WebAssembly."
    }
}

dependencies {
    commonMainApi(project(":kapi-spec:kapi-spec-core"))
    commonMainApi(libs.kotlinx.serialization.json)
}
