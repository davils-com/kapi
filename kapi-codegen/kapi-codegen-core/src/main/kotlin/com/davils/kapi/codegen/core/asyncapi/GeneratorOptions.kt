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

import com.davils.kapi.codegen.core.naming.Identifiers
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.codegen.core.options.GeneratorViolationCodes
import com.davils.kapi.codegen.core.options.Visibility
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

private val generated: Set<Artifact> = setOf(Artifact.Models, Artifact.Client, Artifact.Server)

/**
 * The options of a generator of Kotlin from AsyncAPI documents, built by [GeneratorBuilder].
 *
 * The options are refused when the package is no Kotlin package in lower case, the name of the API or the prefix
 * does not start with a capital letter, no artifact is named, an artifact other than the models, the client and the
 * server is named, or the client or the server is named without the models they build on. Options are immutable and
 * safe to share between threads.
 *
 * @since 1.0.0
 */
public class GeneratorOptions internal constructor(builder: GeneratorBuilder) : Validatable {
    /**
     * The package of the generated code.
     *
     * @since 1.0.0
     */
    public val packageName: String = builder.packageName

    /**
     * The name of the API.
     *
     * @since 1.0.0
     */
    public val apiName: String = builder.apiName

    /**
     * The prefix of every generated type.
     *
     * @since 1.0.0
     */
    public val typePrefix: String = builder.typePrefix

    /**
     * What to generate.
     *
     * @since 1.0.0
     */
    public val artifacts: Set<Artifact> = builder.artifacts.toSet()

    /**
     * The visibility of the generated declarations.
     *
     * @since 1.0.0
     */
    public val visibility: Visibility = builder.visibility

    /**
     * The version of the generator that the provenance records.
     *
     * @since 1.0.0
     */
    public val generatorVersion: String = builder.generatorVersion

    override fun ValidationScope.validate() {
        if (!Identifiers.isPackage(packageName)) {
            fail(::packageName, "must be a package in lower case such as com.example.chat", PACKAGE_INVALID)
        }
        if (!Identifiers.isTypeName(apiName)) {
            fail(::apiName, "must be a name that starts with a capital letter such as Chat", API_NAME_INVALID)
        }
        if (typePrefix.isNotEmpty() && !Identifiers.isTypeName(typePrefix)) {
            fail(::typePrefix, "must be empty or a name that starts with a capital letter", TYPE_PREFIX_INVALID)
        }
        validateArtifacts()
    }

    override fun toString(): String =
        "GeneratorOptions(packageName=$packageName, apiName=$apiName, artifacts=$artifacts)"

    private fun ValidationScope.validateArtifacts() {
        if (artifacts.isEmpty()) {
            fail(::artifacts, "must name at least one artifact", GeneratorViolationCodes.ARTIFACTS_EMPTY)
        }
        val foreign = artifacts.filterNot { artifact -> artifact in generated }
        foreign.forEach { artifact ->
            fail(::artifacts, "names $artifact, which AsyncAPI does not describe", GenerationCodes.ARTIFACT_UNSUPPORTED)
        }
        val sides = artifacts.filter { artifact -> artifact == Artifact.Client || artifact == Artifact.Server }
        val isModelled = Artifact.Models in artifacts
        sides.filterNot { isModelled }.forEach { artifact ->
            fail(::artifacts, "names $artifact, which needs Models", GeneratorViolationCodes.ARTIFACT_MISSING)
        }
    }

    private companion object {
        val PACKAGE_INVALID = GeneratorViolationCodes.PACKAGE_INVALID

        val API_NAME_INVALID = GeneratorViolationCodes.API_NAME_INVALID

        val TYPE_PREFIX_INVALID = GeneratorViolationCodes.TYPE_PREFIX_INVALID
    }
}
