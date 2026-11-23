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
 * AsyncAPI of kapi: the model of AsyncAPI 3 with its WebSocket binding, the strict reader, the rules, traits and the
 * writer.
 */
@SuppressWarnings("module")
module com.davils.kapi.spec.asyncapi {
    requires transitive kotlin.stdlib;
    requires transitive com.davils.kapi.spec.core;
    requires com.davils.kapi.spec.schema;

    exports com.davils.kapi.spec.asyncapi;
    exports com.davils.kapi.spec.asyncapi.model.binding;
    exports com.davils.kapi.spec.asyncapi.model.channel;
    exports com.davils.kapi.spec.asyncapi.model.component;
    exports com.davils.kapi.spec.asyncapi.model.info;
    exports com.davils.kapi.spec.asyncapi.model.message;
    exports com.davils.kapi.spec.asyncapi.model.operation;
    exports com.davils.kapi.spec.asyncapi.model.schema;
    exports com.davils.kapi.spec.asyncapi.model.security;
    exports com.davils.kapi.spec.asyncapi.model.server;
}
