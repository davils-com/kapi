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

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property

/**
 * The `kapi { }` block of a build: the API documents to generate code from.
 *
 * @since 1.0.0
 */
public abstract class CodegenExtension {
    /**
     * The OpenAPI documents of the project.
     *
     * @since 1.0.0
     */
    public abstract val openApi: NamedDomainObjectContainer<SpecificationSpec>

    /**
     * The version of the kapi generator and runtimes to use; the version of the plugin by default.
     *
     * @since 1.0.0
     */
    public abstract val version: Property<String>

    /**
     * Adds the OpenAPI document [name] and configures it.
     *
     * @param name the name of the specification, which names its tasks and output folders.
     * @param configure the configuration.
     * @since 1.0.0
     */
    public fun openApi(name: String, configure: Action<in SpecificationSpec>) {
        openApi.register(name, configure)
    }
}
