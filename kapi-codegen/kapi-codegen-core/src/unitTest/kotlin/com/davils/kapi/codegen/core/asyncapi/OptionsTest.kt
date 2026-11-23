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

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.codegen.core.options.GeneratorViolationCodes
import com.davils.kapi.codegen.core.options.Visibility
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val PACKAGE: String = "com.example.chat"

private const val API: String = "Chat"

private fun codesOf(configure: GeneratorBuilder.() -> Unit): List<Any> {
    val builder = GeneratorBuilder()
    builder.packageName = PACKAGE
    builder.apiName = API
    builder.configure()
    return builder.validate().violations.map { violation -> violation.code }
}

class OptionsTest : FunSpec({
    test("the defaults generate public models without a prefix and render what they generate") {
        val generator = Codegen.asyncApi {
            packageName = PACKAGE
            apiName = API
        }
        val options = generator.options

        options.typePrefix shouldBe ""
        options.artifacts shouldBe setOf(Artifact.Models)
        options.visibility shouldBe Visibility.Public
        options.generatorVersion shouldBe "development"
        generator.toString() shouldContain "packageName=$PACKAGE, apiName=$API, artifacts=[Models]"
    }

    test("the models, the client and the server are accepted together") {
        codesOf { artifacts = setOf(Artifact.Models, Artifact.Client, Artifact.Server) }.shouldBeEmpty()
    }

    test("a package, a name of the api and a prefix that are no Kotlin names are refused") {
        codesOf {
            packageName = "com.Example"
            apiName = "chat"
            typePrefix = "my"
        } shouldContainExactly listOf(
            GeneratorViolationCodes.PACKAGE_INVALID,
            GeneratorViolationCodes.API_NAME_INVALID,
            GeneratorViolationCodes.TYPE_PREFIX_INVALID,
        )
    }

    test("no artifact, an artifact AsyncAPI does not describe and a side without models are refused") {
        codesOf { artifacts = emptySet() } shouldBe listOf(GeneratorViolationCodes.ARTIFACTS_EMPTY)
        codesOf { artifacts = setOf(Artifact.Models, Artifact.Mocks) } shouldBe
            listOf(GenerationCodes.ARTIFACT_UNSUPPORTED)
        codesOf { artifacts = setOf(Artifact.Client) } shouldBe listOf(GeneratorViolationCodes.ARTIFACT_MISSING)
    }

    test("invalid options never build a generator") {
        shouldThrow<ValidationException> { Codegen.asyncApi { apiName = API } }
    }

    test("every code is named once and under codegen.asyncapi") {
        val names = GenerationCodes.all.map { code -> code.value }

        names.toSet().size shouldBe names.size
        names.all { name -> name.startsWith("codegen.asyncapi.") } shouldBe true
    }
})
