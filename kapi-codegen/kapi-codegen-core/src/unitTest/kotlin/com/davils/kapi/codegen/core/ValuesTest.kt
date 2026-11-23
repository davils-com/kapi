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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.codegen.core

import com.davils.kapi.codegen.core.launch.LaunchDiagnostic
import com.davils.kapi.codegen.core.launch.LaunchOutcome
import com.davils.kapi.codegen.core.launch.LaunchReport
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val TYPE: String = "PetstorePet"

private const val OTHER: String = "Other"

private const val FILE: String = "a.kt"

private const val PACKAGE: String = "com.example"

private const val DIGEST: String = "sha-256:00"

private const val DOCUMENT: String = "petstore.yaml"

private val fingerprint: Fingerprint = Fingerprint(name = FILE, digest = DIGEST)

private val model: CatalogModel = CatalogModel(type = TYPE, kind = ModelKind.Object, hasWriteShape = true)

private val catalog: ModelCatalog = ModelCatalog(
    document = DOCUMENT,
    packageName = PACKAGE,
    models = mapOf("#/Pet" to model),
)

private fun provenance(api: String = "Petstore"): Provenance = Provenance(
    protocol = Provenance.PROTOCOL,
    generator = "test",
    api = api,
    packageName = PACKAGE,
    options = "sha-256:01",
    sources = listOf(fingerprint),
    files = listOf(fingerprint),
)

private fun file(path: String = FILE, platform: GeneratedPlatform = GeneratedPlatform.Common): GeneratedFile =
    GeneratedFile(path = path, kind = GeneratedFileKind.Kotlin, content = "", platform = platform)

private fun diagnostic(line: Int? = 1): LaunchDiagnostic =
    LaunchDiagnostic(code = "c", isError = true, message = "m", file = "f", line = line, column = 2)

private fun <T : Any> equality(value: T, same: T, others: List<T>) {
    value shouldBe same
    value.hashCode() shouldBe same.hashCode()
    value shouldNotBe Any()
    others.forEach { other -> value shouldNotBe other }
}

class ValuesTest : FunSpec({
    context("the values of a generation") {
        test("are equal by their parts alone") {
            equality(
                value = model,
                same = CatalogModel(type = TYPE, kind = ModelKind.Object, hasWriteShape = true),
                others = listOf(
                    CatalogModel(type = OTHER, kind = ModelKind.Object, hasWriteShape = true),
                    CatalogModel(type = TYPE, kind = ModelKind.Enum, hasWriteShape = true),
                    CatalogModel(type = TYPE, kind = ModelKind.Object, hasWriteShape = false),
                ),
            )
            equality(
                value = fingerprint,
                same = Fingerprint(name = FILE, digest = DIGEST),
                others = listOf(
                    Fingerprint(name = "b.kt", digest = DIGEST),
                    Fingerprint(name = FILE, digest = "sha-256:ff"),
                ),
            )
            equality(
                value = catalog,
                same = ModelCatalog(document = DOCUMENT, packageName = PACKAGE, models = catalog.models),
                others = listOf(
                    ModelCatalog(document = "other.yaml", packageName = PACKAGE, models = catalog.models),
                    ModelCatalog(document = DOCUMENT, packageName = "com.other", models = catalog.models),
                    ModelCatalog(document = DOCUMENT, packageName = PACKAGE, models = emptyMap()),
                ),
            )
            equality(value = provenance(), same = provenance(), others = listOf(provenance(OTHER)))
            equality(
                value = file(),
                same = file(),
                others = listOf(
                    file(path = "b.kt"),
                    file(platform = GeneratedPlatform.Jvm),
                    GeneratedFile(path = FILE, kind = GeneratedFileKind.Resource, content = ""),
                    GeneratedFile(path = FILE, kind = GeneratedFileKind.Kotlin, content = "x"),
                ),
            )
        }

        test("are equal as generations by every part") {
            val generated = Generation.Generated(
                files = listOf(file()),
                diagnostics = emptyList(),
                provenance = provenance(),
                catalog = catalog,
            )

            equality(
                value = generated,
                same = Generation.Generated(
                    files = listOf(file()),
                    diagnostics = emptyList(),
                    provenance = provenance(),
                    catalog = catalog,
                ),
                others = listOf(
                    Generation.Generated(
                        files = emptyList(),
                        diagnostics = emptyList(),
                        provenance = provenance(),
                        catalog = catalog,
                    ),
                    Generation.Generated(
                        files = listOf(file()),
                        diagnostics = emptyList(),
                        provenance = provenance(OTHER),
                        catalog = catalog,
                    ),
                    Generation.Generated(
                        files = listOf(file()),
                        diagnostics = emptyList(),
                        provenance = provenance(),
                        catalog = ModelCatalog(document = "x", packageName = "y", models = emptyMap()),
                    ),
                ),
            )
            equality(
                value = Generation.Refused(emptyList()),
                same = Generation.Refused(emptyList()),
                others = emptyList(),
            )
        }

        test("show what they hold") {
            model.toString() shouldContain TYPE
            fingerprint.toString() shouldContain FILE
            catalog.toString() shouldContain "1 models"
            provenance().toString() shouldContain "Petstore"
            Generation.Refused(emptyList()).toString() shouldBe "Refused(0 diagnostics)"
        }
    }

    context("the report of a launch") {
        test("is equal by its parts alone") {
            val report = LaunchReport(
                outcome = LaunchOutcome.Generated,
                reason = null,
                diagnostics = listOf(diagnostic()),
                files = listOf(FILE),
            )

            equality(
                value = report,
                same = LaunchReport(
                    outcome = LaunchOutcome.Generated,
                    reason = null,
                    diagnostics = listOf(diagnostic()),
                    files = listOf(FILE),
                ),
                others = listOf(
                    LaunchReport(
                        outcome = LaunchOutcome.Refused,
                        reason = null,
                        diagnostics = listOf(diagnostic()),
                        files = listOf(FILE),
                    ),
                    LaunchReport(
                        outcome = LaunchOutcome.Generated,
                        reason = "why",
                        diagnostics = listOf(diagnostic()),
                        files = listOf(FILE),
                    ),
                    LaunchReport(
                        outcome = LaunchOutcome.Generated,
                        reason = null,
                        diagnostics = listOf(diagnostic(line = 2)),
                        files = listOf(FILE),
                    ),
                    LaunchReport(
                        outcome = LaunchOutcome.Generated,
                        reason = null,
                        diagnostics = listOf(diagnostic()),
                        files = emptyList(),
                    ),
                ),
            )
            equality(value = diagnostic(), same = diagnostic(), others = listOf(diagnostic(line = null)))
            report.toString() shouldContain "Generated"
        }
    }
})
