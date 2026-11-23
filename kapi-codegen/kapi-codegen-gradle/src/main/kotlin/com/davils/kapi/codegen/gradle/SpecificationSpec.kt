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
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Nested

/**
 * An OpenAPI document and what to generate from it.
 *
 * ```kotlin
 * kapi {
 *     openApi("petstore") {
 *         packageName = "com.example.petstore"
 *         overlay("petstore.overlay.yaml")
 *         acknowledge("openapi.operation.id-missing", "The vendor names no operations")
 *         artifacts {
 *             client = true
 *         }
 *     }
 * }
 * ```
 *
 * By default the document is `src/main/openapi/<name>.yaml`; everything else defaults as [DocumentSpec] describes.
 *
 * @since 1.0.0
 */
public abstract class SpecificationSpec : DocumentSpec() {
    /**
     * The overlays to apply, by their path relative to [root], in order.
     *
     * @since 1.0.0
     */
    public abstract val overlays: ListProperty<String>

    /**
     * Which artifacts to generate.
     *
     * @since 1.0.0
     */
    @get:Nested
    public abstract val artifacts: ArtifactsSpec

    /**
     * Configures which artifacts to generate.
     *
     * @param configure the configuration.
     * @since 1.0.0
     */
    public fun artifacts(configure: Action<in ArtifactsSpec>) {
        configure.execute(artifacts)
    }

    /**
     * Applies the overlay at [path], relative to [root], after the overlays added before.
     *
     * @param path the path of the overlay.
     * @since 1.0.0
     */
    public fun overlay(path: String) {
        overlays.add(path)
    }

    override fun kind(): DocumentKind = DocumentKind.OpenApi

    override fun artifactNames(): Set<String> = artifacts.names()

    override fun overlayPaths(): List<String> = overlays.get()
}
