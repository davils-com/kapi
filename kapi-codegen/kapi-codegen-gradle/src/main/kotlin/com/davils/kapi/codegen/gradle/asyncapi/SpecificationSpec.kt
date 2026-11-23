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

package com.davils.kapi.codegen.gradle.asyncapi

import com.davils.kapi.codegen.gradle.DocumentKind
import com.davils.kapi.codegen.gradle.DocumentSpec
import org.gradle.api.Action
import org.gradle.api.tasks.Nested

/**
 * An AsyncAPI 3 document and what to generate from its WebSocket channels.
 *
 * ```kotlin
 * kapi {
 *     asyncApi("chat") {
 *         packageName = "com.example.chat"
 *         artifacts {
 *             client = true
 *             server = true
 *         }
 *     }
 * }
 * ```
 *
 * By default the document is `src/main/asyncapi/<name>.yaml`; everything else defaults as [DocumentSpec] describes.
 * An AsyncAPI document takes no overlays.
 *
 * @since 1.0.0
 */
public abstract class SpecificationSpec : DocumentSpec() {
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

    override fun kind(): DocumentKind = DocumentKind.AsyncApi

    override fun artifactNames(): Set<String> = artifacts.names()

    override fun overlayPaths(): List<String> = emptyList()
}
