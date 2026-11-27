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
        description = "The runtime core of kapi, the API toolkit of Davils: what generated clients and servers run " +
            "on, codecs, wire types, parameter styles, content negotiation, HTTP semantics, credentials, problem " +
            "details, validation and the destination policy, for the JVM and WebAssembly."
    }
}

dependencies {
    commonMainApi(libs.kore.core)
    commonMainApi(libs.konnect.internet.protocol)
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(libs.kotlinx.io.bytestring)
    commonMainApi(libs.kotlinx.serialization.json)
}
