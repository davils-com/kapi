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

package com.davils.kapi.codegen.core

import com.davils.kapi.codegen.core.options.GeneratorBuilder

/**
 * The entry point of the code generator of kapi.
 *
 * ```kotlin
 * val generator = Codegen.openApi {
 *     packageName = "com.example.petstore"
 *     apiName = "Petstore"
 *     artifacts(Artifact.Models, Artifact.Client)
 * }
 * val generation = generator.generate(reading.document, reading.sources)
 * ```
 *
 * The object holds no state; every generator carries its own options and is safe to share between threads.
 *
 * @since 1.0.0
 */
public object Codegen {
    /**
     * Returns a generator of Kotlin from OpenAPI documents, configured by [configure].
     *
     * @param configure sets the package, the name of the API, the artifacts and the visibility of the generated code.
     * @return the generator.
     * @throws IllegalArgumentException if the options are invalid, such as a package name that is no Kotlin package.
     * @since 1.0.0
     */
    public fun openApi(configure: GeneratorBuilder.() -> Unit): Generator {
        val builder = GeneratorBuilder()
        builder.configure()
        return Generator(builder.build())
    }
}
