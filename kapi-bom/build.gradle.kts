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
    id("kapi.bom")
}

kreate {
    project {
        description = "The bill of materials of kapi: one version for every kapi module."
    }
}

dependencies {
    constraints {
        api(project(":kapi-spec:kapi-spec-core"))
        api(project(":kapi-spec:kapi-spec-schema"))
        api(project(":kapi-spec:kapi-spec-openapi"))
        api(project(":kapi-spec:kapi-spec-asyncapi"))
        api(project(":kapi-spec:kapi-spec-testing"))
        api(project(":kapi-runtime:kapi-runtime-core"))
        api(project(":kapi-runtime:kapi-runtime-client"))
        api(project(":kapi-runtime:kapi-runtime-xml"))
        api(project(":kapi-runtime:kapi-runtime-server"))
        api(project(":kapi-runtime:kapi-runtime-server-ipc"))
        api(project(":kapi-runtime:kapi-runtime-testing"))
        api(project(":kapi-codegen:kapi-codegen-core"))
        api(project(":kapi-codegen:kapi-codegen-gradle"))
    }
}
