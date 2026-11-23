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

package com.davils.kapi.codegen.core.options

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures a generator; [com.davils.kapi.codegen.core.Codegen.openApi] builds the options and refuses invalid ones
 * with a [com.davils.kore.core.validation.report.ValidationException]. A builder is not safe to share between threads.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class GeneratorBuilder internal constructor() :
    ValidatingBuilder<GeneratorOptions>() {
    override val subject: String get() = "openapi generator"

    /**
     * The package of the generated code, such as `com.example.petstore`.
     *
     * @since 1.0.0
     */
    public var packageName: String = ""

    /**
     * The name of the API, a Kotlin type name such as `Petstore`, which every generated type starts with.
     *
     * @since 1.0.0
     */
    public var apiName: String = ""

    /**
     * A prefix in front of the name of the API in every generated type; empty by default.
     *
     * @since 1.0.0
     */
    public var typePrefix: String = ""

    /**
     * What the generation emits.
     *
     * @since 1.0.0
     */
    public var artifacts: Set<Artifact> = setOf(Artifact.Models)

    /**
     * The visibility of the generated declarations.
     *
     * @since 1.0.0
     */
    public var visibility: Visibility = Visibility.Public

    /**
     * The version of the generator, recorded in the provenance.
     *
     * @since 1.0.0
     */
    public var generatorVersion: String = "development"

    override fun assemble(): GeneratorOptions = GeneratorOptions(this)
}
