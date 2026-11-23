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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.codegen.core.options.Visibility
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures the [GeneratorOptions] of a generator of Kotlin from AsyncAPI documents.
 *
 * A builder is not thread-safe; the options it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class GeneratorBuilder internal constructor() : ValidatingBuilder<GeneratorOptions>() {
    override val subject: String get() = "asyncapi generator"

    /**
     * The package of the generated code, such as `com.example.chat`; required.
     *
     * @since 1.0.0
     */
    public var packageName: String = ""

    /**
     * The name of the API, which starts the name of every generated type, such as `Chat`; required.
     *
     * @since 1.0.0
     */
    public var apiName: String = ""

    /**
     * A prefix before the name of the API in every generated type; none by default.
     *
     * @since 1.0.0
     */
    public var typePrefix: String = ""

    /**
     * What to generate: [Artifact.Models], [Artifact.Client] and [Artifact.Server]; the models by default.
     *
     * @since 1.0.0
     */
    public var artifacts: Set<Artifact> = setOf(Artifact.Models)

    /**
     * The visibility of the generated declarations; public by default.
     *
     * @since 1.0.0
     */
    public var visibility: Visibility = Visibility.Public

    /**
     * The version of the generator that the provenance records; `development` by default.
     *
     * @since 1.0.0
     */
    public var generatorVersion: String = "development"

    override fun assemble(): GeneratorOptions = GeneratorOptions(this)
}
