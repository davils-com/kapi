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

import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kapi.codegen.core.naming.Identifiers

private val requirements: Map<Artifact, Artifact> = mapOf(
    Artifact.Client to Artifact.Models,
    Artifact.Server to Artifact.Models,
    Artifact.Mocks to Artifact.Server,
    Artifact.Senders to Artifact.Models,
    Artifact.Receivers to Artifact.Models,
)

public class GeneratorOptions internal constructor(builder: GeneratorBuilder) : Validatable {
    public val packageName: String = builder.packageName

    public val apiName: String = builder.apiName

    public val typePrefix: String = builder.typePrefix

    public val artifacts: Set<Artifact> = builder.artifacts.toSet()

    public val visibility: Visibility = builder.visibility

    public val generatorVersion: String = builder.generatorVersion

    override fun ValidationScope.validate() {
        if (!Identifiers.isPackage(packageName)) {
            fail(
                ::packageName,
                "must be a package in lower case such as com.example.api",
                GeneratorViolationCodes.PACKAGE_INVALID,
            )
        }
        if (!Identifiers.isTypeName(apiName)) {
            fail(
                ::apiName,
                "must be a name that starts with a capital letter such as Petstore",
                GeneratorViolationCodes.API_NAME_INVALID,
            )
        }
        if (typePrefix.isNotEmpty() && !Identifiers.isTypeName(typePrefix)) {
            fail(
                ::typePrefix,
                "must be empty or a name that starts with a capital letter such as Kide",
                GeneratorViolationCodes.TYPE_PREFIX_INVALID,
            )
        }
        validateArtifacts()
    }

    override fun toString(): String =
        "GeneratorOptions(packageName=$packageName, apiName=$apiName, artifacts=$artifacts)"

    private fun ValidationScope.validateArtifacts() {
        if (artifacts.isEmpty()) {
            fail(::artifacts, "must name at least one artifact", GeneratorViolationCodes.ARTIFACTS_EMPTY)
        }
        val missing = requirements.filter { (artifact, needed) -> artifact in artifacts && needed !in artifacts }
        missing.forEach { (artifact, needed) ->
            fail(
                ::artifacts,
                "names $artifact, which needs $needed",
                GeneratorViolationCodes.ARTIFACT_MISSING,
            )
        }
    }
}
