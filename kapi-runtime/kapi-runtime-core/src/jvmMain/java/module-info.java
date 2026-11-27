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
 * The runtime core of kapi: codecs, wire types, parameter styles, content negotiation, HTTP semantics, credentials,
 * problem details, validation and the destination policy that generated clients and servers run on.
 */
@SuppressWarnings({"module", "requires-automatic"})
module com.davils.kapi.runtime.core {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;
    requires transitive kotlinx.io.bytestring;
    requires transitive kotlinx.serialization.core;
    requires transitive kotlinx.serialization.json;
    requires transitive com.davils.kore.core;
    requires com.davils.konnect.core;
    requires com.davils.konnect.internetprotocol;

    exports com.davils.kapi.runtime.core;
    exports com.davils.kapi.runtime.core.cache;
    exports com.davils.kapi.runtime.core.codec;
    exports com.davils.kapi.runtime.core.codec.form;
    exports com.davils.kapi.runtime.core.codec.multipart;
    exports com.davils.kapi.runtime.core.conditional;
    exports com.davils.kapi.runtime.core.destination;
    exports com.davils.kapi.runtime.core.digest;
    exports com.davils.kapi.runtime.core.framing;
    exports com.davils.kapi.runtime.core.header;
    exports com.davils.kapi.runtime.core.negotiation;
    exports com.davils.kapi.runtime.core.parameter;
    exports com.davils.kapi.runtime.core.pattern;
    exports com.davils.kapi.runtime.core.problem;
    exports com.davils.kapi.runtime.core.range;
    exports com.davils.kapi.runtime.core.ratelimit;
    exports com.davils.kapi.runtime.core.response;
    exports com.davils.kapi.runtime.core.security;
    exports com.davils.kapi.runtime.core.structured;
    exports com.davils.kapi.runtime.core.validation;
    exports com.davils.kapi.runtime.core.wire;
}
