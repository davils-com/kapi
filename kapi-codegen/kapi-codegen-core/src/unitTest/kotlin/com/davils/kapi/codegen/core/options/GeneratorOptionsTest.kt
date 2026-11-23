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

import com.davils.kapi.codegen.core.fixture.API
import com.davils.kapi.codegen.core.fixture.PACKAGE
import com.davils.kapi.codegen.core.fixture.generator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private fun codesOf(configure: GeneratorBuilder.() -> Unit): List<Any> {
    val builder = GeneratorBuilder()
    builder.packageName = PACKAGE
    builder.apiName = API
    builder.configure()
    return builder.validate().violations.map { violation -> violation.code }
}

class GeneratorOptionsTest : FunSpec({
    context("the defaults") {
        test("generate public models without a prefix") {
            val options = generator().options

            options.packageName shouldBe PACKAGE
            options.apiName shouldBe API
            options.typePrefix shouldBe ""
            options.artifacts shouldBe setOf(Artifact.Models)
            options.visibility shouldBe Visibility.Public
            codesOf { }.shouldBeEmpty()
        }

        test("render the package, the api and the artifacts") {
            generator().toString() shouldContain "packageName=$PACKAGE, apiName=$API, artifacts=[Models]"
        }
    }

    context("validation") {
        test("accepts no prefix and every artifact together") {
            codesOf {
                typePrefix = ""
                artifacts = Artifact.entries.toSet()
            }.shouldBeEmpty()
        }

        test("reports a package that is none") {
            val invalid = listOf(GeneratorViolationCodes.PACKAGE_INVALID)

            codesOf { packageName = "" } shouldBe invalid
            codesOf { packageName = "com.Example" } shouldBe invalid
            codesOf { packageName = "com.class.api" } shouldBe invalid
        }

        test("reports a name of the api and a prefix that are no type names") {
            codesOf { apiName = "petstore" } shouldBe listOf(GeneratorViolationCodes.API_NAME_INVALID)
            codesOf { typePrefix = "kide" } shouldBe listOf(GeneratorViolationCodes.TYPE_PREFIX_INVALID)
        }

        test("reports every problem at once") {
            val codes = codesOf {
                packageName = "Com"
                apiName = ""
                typePrefix = "1"
                artifacts = emptySet()
            }

            codes shouldBe listOf(
                GeneratorViolationCodes.PACKAGE_INVALID,
                GeneratorViolationCodes.API_NAME_INVALID,
                GeneratorViolationCodes.TYPE_PREFIX_INVALID,
                GeneratorViolationCodes.ARTIFACTS_EMPTY,
            )
        }

        test("reports an artifact whose foundation is missing") {
            val missing = GeneratorViolationCodes.ARTIFACT_MISSING

            codesOf { artifacts = setOf(Artifact.Client) } shouldBe listOf(missing)
            codesOf { artifacts = setOf(Artifact.Server) } shouldBe listOf(missing)
            codesOf { artifacts = setOf(Artifact.Senders) } shouldBe listOf(missing)
            codesOf { artifacts = setOf(Artifact.Receivers) } shouldBe listOf(missing)
            codesOf { artifacts = setOf(Artifact.Models, Artifact.Mocks) } shouldBe
                listOf(missing)
        }
    }
})
