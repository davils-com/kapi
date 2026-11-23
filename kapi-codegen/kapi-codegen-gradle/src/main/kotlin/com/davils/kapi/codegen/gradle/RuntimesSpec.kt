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

package com.davils.kapi.codegen.gradle

import org.gradle.api.provider.Property

/**
 * The runtimes the plugin adds to the project: `com.davils:kapi-runtime-core` for every document, the client runtime
 * for clients and senders and the server runtime for servers, mocks and receivers, all at
 * [CodegenExtension.version].
 *
 * @since 1.0.0
 */
public interface RuntimesSpec {
    /**
     * Whether the plugin adds the runtimes; `true` by default. A build that declares the runtimes itself, such as
     * from a mirror under other coordinates or as files, turns it off.
     *
     * @since 1.0.0
     */
    public val enabled: Property<Boolean>
}
