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
import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

/**
 * What every API document of the `kapi { }` block shares: where it lies, the package and names of the generated code
 * and the warnings the build accepts.
 *
 * The name of a document names its tasks, `kapiGenerate<Name>` and `kapiLint<Name>`, and its output folders; it must
 * be unique across `openApi` and `asyncApi`. By default the document is `src/main/<openapi|asyncapi>/<name>.yaml`,
 * the root its folder, the API name the name of the document in upper camel case, and the generated code is public
 * and carries no prefix.
 *
 * @since 1.0.0
 */
public abstract class DocumentSpec : Named {
    /**
     * The document to read.
     *
     * @since 1.0.0
     */
    public abstract val source: RegularFileProperty

    /**
     * The folder the document and every document it references must stay inside. A reference that leaves it, or one
     * to anything but a local file, is refused with a diagnostic.
     *
     * @since 1.0.0
     */
    public abstract val root: DirectoryProperty

    /**
     * The package of the generated code, such as `com.example.petstore`; required.
     *
     * @since 1.0.0
     */
    public abstract val packageName: Property<String>

    /**
     * The name of the API, which the generated clients, services and their files are named after.
     *
     * @since 1.0.0
     */
    public abstract val apiName: Property<String>

    /**
     * A prefix of every generated type; empty by default.
     *
     * @since 1.0.0
     */
    public abstract val typePrefix: Property<String>

    /**
     * The visibility of the generated declarations.
     *
     * @since 1.0.0
     */
    public abstract val visibility: Property<Visibility>

    /**
     * The codes of warnings the build accepts, each with its reason.
     *
     * @since 1.0.0
     */
    public abstract val acknowledgements: MapProperty<String, String>

    /**
     * The check of this document against an earlier version of it.
     *
     * @since 1.0.0
     */
    @get:Nested
    public abstract val compatibility: CompatibilitySpec

    init {
        typePrefix.convention("")
        visibility.convention(Visibility.Public)
    }

    /**
     * Accepts the warnings of [code] for [reason]. An acknowledgement without a reason, or one that no warning
     * matches, fails the build, so no acknowledgement outlives the warning it was written for.
     *
     * @param code the code of the warning, such as `openapi.version.upgraded`.
     * @param reason why the warning is accepted.
     * @since 1.0.0
     */
    public fun acknowledge(code: String, reason: String) {
        acknowledgements.put(code, reason)
    }

    /**
     * Configures [compatibility].
     *
     * @param configure the configuration.
     * @since 1.0.0
     */
    public fun compatibility(configure: Action<in CompatibilitySpec>) {
        configure.execute(compatibility)
    }

    internal abstract fun kind(): DocumentKind

    internal abstract fun artifactNames(): Set<String>

    internal abstract fun overlayPaths(): List<String>
}
