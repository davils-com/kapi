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
 * The client runtime of kapi: calls of generated clients over the Ktor HttpClient of the application with credentials,
 * OAuth 2, DPoP, redirects, streams, server-sent events, pages, resilience, telemetry and WebSocket channels.
 */
@SuppressWarnings({"module", "requires-automatic", "requires-transitive-automatic"})
module com.davils.kapi.runtime.client {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;
    requires transitive kotlinx.io.core;
    requires transitive kotlinx.serialization.core;
    requires transitive kotlinx.serialization.json;
    requires transitive com.davils.kore.core;
    requires transitive com.davils.kapi.runtime.core;
    requires transitive com.davils.kollect.core;
    requires transitive com.davils.konfetti.core;
    requires transitive com.davils.kontinuity.core;
    requires transitive com.davils.kontinuity.policy;
    requires transitive com.davils.kontinuity.retry;
    requires transitive com.davils.kontinuity.circuitbreaker;
    requires transitive com.davils.kontinuity.cache;
    requires transitive com.davils.krypt.core;
    requires transitive com.davils.krypt.primitive;
    requires transitive com.davils.krypt.jose;
    requires transitive io.ktor.client.core;
    requires transitive io.ktor.http;
    requires transitive io.ktor.io;
    requires transitive io.ktor.utils;
    requires transitive io.ktor.websockets;

    exports com.davils.kapi.runtime.client;
    exports com.davils.kapi.runtime.client.call;
    exports com.davils.kapi.runtime.client.channel;
    exports com.davils.kapi.runtime.client.conditional;
    exports com.davils.kapi.runtime.client.dpop;
    exports com.davils.kapi.runtime.client.oauth;
    exports com.davils.kapi.runtime.client.page;
    exports com.davils.kapi.runtime.client.pipeline;
    exports com.davils.kapi.runtime.client.range;
    exports com.davils.kapi.runtime.client.redirect;
    exports com.davils.kapi.runtime.client.retry;
    exports com.davils.kapi.runtime.client.stream;
}
