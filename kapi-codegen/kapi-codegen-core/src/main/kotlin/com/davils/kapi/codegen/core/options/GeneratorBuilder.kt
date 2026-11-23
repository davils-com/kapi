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

@DavilsDsl
public class GeneratorBuilder internal constructor() :
    ValidatingBuilder<GeneratorOptions>() {
    override val subject: String get() = "openapi generator"

    public var packageName: String = ""

    public var apiName: String = ""

    public var typePrefix: String = ""

    public var artifacts: Set<Artifact> = setOf(Artifact.Models)

    public var visibility: Visibility = Visibility.Public

    public var generatorVersion: String = "development"

    override fun assemble(): GeneratorOptions = GeneratorOptions(this)
}
