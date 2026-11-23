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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import com.davils.kapi.codegen.gradle.asyncapi.SpecificationSpec as ChannelSpecificationSpec

/**
 * The `kapi { }` block of a build: the API documents to generate Kotlin from, the generator and the runtimes.
 *
 * ```kotlin
 * plugins {
 *     kotlin("multiplatform")
 *     kotlin("plugin.serialization")
 *     id("com.davils.kapi")
 * }
 *
 * kapi {
 *     openApi("petstore") {
 *         packageName = "com.example.petstore"
 *         overlay("petstore.overlay.yaml")
 *         acknowledge("openapi.version.upgraded", "The vendor publishes Swagger 2.0 only")
 *         artifacts {
 *             client = true
 *             server = true
 *         }
 *     }
 *     asyncApi("chat") {
 *         packageName = "com.example.chat"
 *         artifacts {
 *             client = true
 *         }
 *     }
 * }
 * ```
 *
 * **Documents.** [openApi] reads `src/main/openapi/<name>.yaml` and [asyncApi] reads `src/main/asyncapi/<name>.yaml`
 * unless the document sets its `source`; see [DocumentSpec], [SpecificationSpec] and
 * [com.davils.kapi.codegen.gradle.asyncapi.SpecificationSpec]. Every name must be unique across both containers: a
 * name that another document already takes fails the build when it is added.
 *
 * **Tasks.** For every document the plugin registers, in the group `kapi`:
 *
 * - `kapiGenerate<Name>` generates the chosen artifacts into `build/generated/kapi/<name>` and writes
 *   `build/reports/kapi/<name>/report.json` and `report.sarif`;
 * - `kapiLint<Name>` reads and audits the document exactly as a generation does, writes no code and writes
 *   `build/reports/kapi/<name>/lint.json` and `lint.sarif`.
 *
 * Both fail on every error and on every warning that no acknowledgement with a reason accepts, and on every
 * acknowledgement without a reason or without a matching warning; the failure lists the rendered diagnostics, such
 * as `petstore.yaml:12:7: error: [openapi.field.unknown] ...`. Both are cacheable and relocatable: they read the
 * document and every file below its root by their relative paths. `kapiLint` runs every lint, and `kapiCheck` runs
 * every check of kapi and is part of `check` when the `base` plugin is applied. A comparison against a baseline
 * document is not offered yet.
 *
 * **Source sets.** In a multiplatform project the models, clients and senders go to `commonMain` and the servers,
 * mocks and receivers to `jvmMain`; in a JVM project everything goes to `main`. The generated models need the
 * kotlinx.serialization compiler plugin, `kotlin("plugin.serialization")`, which the build applies itself.
 *
 * **Generator and runtimes.** The generator runs in a worker process of its own with at most 512 MiB of heap, on the
 * Java toolchain of the project, and on a class path that never meets the build's: `com.davils:kapi-codegen-core` at
 * [version], resolved from the repositories of the project, unless [generatorClasspath] names one. The plugin adds the
 * runtimes the chosen artifacts need at [version] unless [runtimes] turns them off.
 *
 * **Security.** A document, its overlays and every document they reference are read from local files below the root
 * of the document only, within the limits of the readers of kapi; nothing is fetched from the network. The generator
 * escapes every text of a document where it writes it and refuses every name it cannot make a Kotlin identifier.
 *
 * The extension is configured while the build is configured and is not safe to share between threads.
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
     * The AsyncAPI documents of the project.
     *
     * @since 1.0.0
     */
    public abstract val asyncApi: NamedDomainObjectContainer<ChannelSpecificationSpec>

    /**
     * The version of the kapi generator and runtimes to use; the version of the plugin by default.
     *
     * @since 1.0.0
     */
    public abstract val version: Property<String>

    /**
     * The class path of the generator. By default `com.davils:kapi-codegen-core` at [version] with its dependencies,
     * resolved from the repositories of the project; a build that sets it, such as to run a generator built from
     * source, replaces it entirely. It is read when a task of kapi is configured, so the `kapi { }` block sets it.
     *
     * @since 1.0.0
     */
    public abstract val generatorClasspath: ConfigurableFileCollection

    /**
     * The runtimes the plugin adds.
     *
     * @since 1.0.0
     */
    @get:Nested
    public abstract val runtimes: RuntimesSpec

    /**
     * Adds the OpenAPI document [name] and configures it.
     *
     * @param name the name of the document, which names its tasks and output folders.
     * @param configure the configuration.
     * @throws org.gradle.api.InvalidUserDataException if another document already takes the name.
     * @since 1.0.0
     */
    public fun openApi(name: String, configure: Action<in SpecificationSpec>) {
        openApi.register(name, configure)
    }

    /**
     * Adds the AsyncAPI document [name] and configures it.
     *
     * @param name the name of the document, which names its tasks and output folders.
     * @param configure the configuration.
     * @throws org.gradle.api.InvalidUserDataException if another document already takes the name.
     * @since 1.0.0
     */
    public fun asyncApi(name: String, configure: Action<in ChannelSpecificationSpec>) {
        asyncApi.register(name, configure)
    }

    /**
     * Configures the runtimes the plugin adds.
     *
     * @param configure the configuration.
     * @since 1.0.0
     */
    public fun runtimes(configure: Action<in RuntimesSpec>) {
        configure.execute(runtimes)
    }
}
