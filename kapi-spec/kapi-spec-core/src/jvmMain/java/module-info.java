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
 * The specification core of kapi: sources and resolvers of API documents, located diagnostics, references,
 * extensions and exact values.
 */
@SuppressWarnings("module")
module com.davils.kapi.spec.core {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;
    requires transitive kotlinx.io.bytestring;
    requires transitive com.davils.kore.core;
    requires transitive com.davils.kaml.core;
    requires transitive com.davils.kaml.document;
    requires com.davils.krypt.primitive;

    exports com.davils.kapi.spec.core.collection;
    exports com.davils.kapi.spec.core.compatibility;
    exports com.davils.kapi.spec.core.diagnostic;
    exports com.davils.kapi.spec.core.load;
    exports com.davils.kapi.spec.core.model;
    exports com.davils.kapi.spec.core.policy;
    exports com.davils.kapi.spec.core.source;
    exports com.davils.kapi.spec.core.value;
}
