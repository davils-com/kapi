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
        description = "The XML codec of kapi, the API toolkit of Davils: content of application/xml, text/xml and " +
            "every +xml type, read strictly with the hardened reader of kxml within limits and without document " +
            "type declarations or external entities, for the JVM and WebAssembly."
    }
}

dependencies {
    commonMainApi(project(":kapi-runtime:kapi-runtime-core"))
    commonMainApi(libs.kxml.core)
    commonMainApi(libs.kxml.serialization)
}
