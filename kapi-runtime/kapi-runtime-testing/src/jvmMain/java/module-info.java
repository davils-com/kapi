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
 * The test support of kapi: fake credentials and a fake OAuth 2 authorization server, a deterministic clock and
 * entropy, a mock HTTP API on the Ktor mock engine and contract assertions of recorded exchanges against OpenAPI
 * documents, and on the JVM calls of the operations of generated servers without Ktor. It is never on a production
 * class path.
 */
@SuppressWarnings({"module", "requires-automatic", "requires-transitive-automatic"})
module com.davils.kapi.runtime.testing {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;
    requires transitive kotlinx.io.bytestring;
    requires transitive kotlinx.serialization.core;
    requires transitive kotlinx.serialization.json;
    requires transitive com.davils.kore.core;
    requires transitive com.davils.kaml.document;
    requires transitive com.davils.krypt.core;
    requires transitive com.davils.krypt.primitive;
    requires transitive com.davils.kapi.runtime.core;
    requires transitive com.davils.kapi.runtime.client;
    requires transitive com.davils.kapi.runtime.server;
    requires transitive com.davils.kapi.spec.core;
    requires transitive com.davils.kapi.spec.openapi;
    requires transitive com.davils.kapi.spec.schema;
    requires transitive io.ktor.client.core;
    requires transitive io.ktor.client.mock;
    requires transitive io.ktor.http;
    requires transitive io.ktor.io;
    requires transitive io.ktor.utils;

    exports com.davils.kapi.runtime.testing;
    exports com.davils.kapi.runtime.testing.contract;
    exports com.davils.kapi.runtime.testing.fake;
    exports com.davils.kapi.runtime.testing.mock;
    exports com.davils.kapi.runtime.testing.oauth;
    exports com.davils.kapi.runtime.testing.server;
}
