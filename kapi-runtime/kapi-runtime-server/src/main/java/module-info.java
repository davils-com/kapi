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
 * The server runtime of kapi: the Ktor application plugin that mounts generated services, reads and checks requests
 * as a whole, negotiates content, streams, authorizes callers, verifies deliveries, answers with problem details and
 * serves WebSocket channels.
 */
@SuppressWarnings({"module", "requires-automatic", "requires-transitive-automatic"})
module com.davils.kapi.runtime.server {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;
    requires transitive kotlinx.io.bytestring;
    requires transitive kotlinx.serialization.core;
    requires transitive kotlinx.serialization.json;
    requires transitive com.davils.kore.core;
    requires transitive com.davils.kapi.runtime.core;
    requires transitive com.davils.kollect.core;
    requires transitive com.davils.konfetti.core;
    requires transitive com.davils.krypt.jose;
    requires transitive com.davils.krypt.x509;
    requires transitive io.ktor.server.core;
    requires transitive io.ktor.server.websockets;
    requires transitive io.ktor.server.sse;
    requires transitive io.ktor.http;
    requires transitive io.ktor.io;
    requires transitive io.ktor.utils;
    requires transitive io.ktor.websockets;

    exports com.davils.kapi.runtime.server;
    exports com.davils.kapi.runtime.server.channel;
    exports com.davils.kapi.runtime.server.operation;
    exports com.davils.kapi.runtime.server.problem;
    exports com.davils.kapi.runtime.server.reply;
    exports com.davils.kapi.runtime.server.request;
    exports com.davils.kapi.runtime.server.security;
    exports com.davils.kapi.runtime.server.telemetry;
    exports com.davils.kapi.runtime.server.webhook;
}
