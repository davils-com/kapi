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
 * JSON Schema of kapi: a bounded validator of JSON Schema with the dialects of OpenAPI, a linear-time pattern engine
 * and format assertions.
 */
@SuppressWarnings({"module", "requires-automatic"})
module com.davils.kapi.spec.schema {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.serialization.core;
    requires transitive kotlinx.serialization.json;
    requires transitive com.davils.kore.core;
    requires transitive com.davils.kaml.core;
    requires transitive com.davils.kaml.document;
    requires transitive com.davils.kapi.spec.core;
    requires transitive com.davils.kapi.runtime.core;
    requires com.davils.konnect.internetprotocol;

    exports com.davils.kapi.spec.schema;
    exports com.davils.kapi.spec.schema.format;
}
