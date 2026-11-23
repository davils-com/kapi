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
/**
 * The code generator of kapi: audits an OpenAPI document, names what it declares and emits Kotlin models, clients,
 * servers, mocks and webhooks with KotlinPoet, with the provenance of every file, and runs as the worker of the
 * Gradle plugin.
 */
@SuppressWarnings({"module", "requires-automatic"})
module com.davils.kapi.codegen.core {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.serialization.core;
    requires transitive com.davils.kore.core;
    requires transitive com.davils.kapi.spec.core;
    requires transitive com.davils.kapi.spec.openapi;
    requires kotlinx.coroutines.core;
    requires kotlinx.io.bytestring;
    requires kotlinx.serialization.json;
    requires com.squareup.kotlinpoet;

    exports com.davils.kapi.codegen.core;
    exports com.davils.kapi.codegen.core.launch;
    exports com.davils.kapi.codegen.core.options;
}
