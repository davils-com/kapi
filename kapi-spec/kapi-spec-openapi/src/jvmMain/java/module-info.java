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
 * OpenAPI of kapi: the model of OpenAPI 3.0, 3.1 and 3.2, the strict reader, the upgrade of Swagger 2.0, the rules
 * and the writer.
 */
@SuppressWarnings("module")
module com.davils.kapi.spec.openapi {
    requires transitive kotlin.stdlib;
    requires transitive com.davils.kapi.spec.core;

    exports com.davils.kapi.spec.openapi;
    exports com.davils.kapi.spec.openapi.model.component;
    exports com.davils.kapi.spec.openapi.model.content;
    exports com.davils.kapi.spec.openapi.model.info;
    exports com.davils.kapi.spec.openapi.model.parameter;
    exports com.davils.kapi.spec.openapi.model.path;
    exports com.davils.kapi.spec.openapi.model.schema;
    exports com.davils.kapi.spec.openapi.model.security;
    exports com.davils.kapi.spec.openapi.model.server;
}
