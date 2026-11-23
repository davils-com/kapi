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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.ModelCatalog
import com.davils.kapi.codegen.core.fixture.KotlinCompilation
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.testing.MemoryResolver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf

private const val LINE: String = "\n"

private val head: String =
    listOf("openapi: 3.1.0", "info: {title: Api, version: 1.0.0}", "components:", "  schemas:", "").joinToString(LINE)

private const val SHARED: String = "    Account: {type: object, properties: {id: {type: string, readOnly: true}}}"

private const val TO_ACCOUNT: String = "{\$ref: 'shared.yaml#/components/schemas/Account'}"

private const val SHOP: String = "    Order: {type: object, properties: {buyer: $TO_ACCOUNT}}"

private val sharedUri: DocumentUri = DocumentUri("file:/work/specs/shared.yaml")

private val shopUri: DocumentUri = DocumentUri("file:/work/specs/shop.yaml")

private suspend fun generated(api: String, source: Source.Text, catalogs: List<ModelCatalog>): Generation.Generated {
    val reader = OpenApi.reader { resolver = MemoryResolver(mapOf(sharedUri to head + SHARED + LINE)) }
    val read = reader.read(source).shouldBeInstanceOf<Reading.Read>()
    val generator = Codegen.openApi {
        packageName = "com.example." + api.lowercase()
        apiName = api
    }
    val generation = generator.generate(read.document, read.sources, catalogs)
    return generation.shouldBeInstanceOf<Generation.Generated>()
}

class CatalogCompilationTest : FunSpec({
    context("two documents of which one uses the models of the other") {
        test("compile together, with a shape to read and a shape to write on both sides") {
            val shared = generated("Shared", Source.Text(head + SHARED + LINE, sharedUri), emptyList())
            val shop = generated("Shop", Source.Text(head + SHOP + LINE, shopUri), listOf(shared.catalog))

            KotlinCompilation.problems(shared.files + shop.files).shouldBeEmpty()
        }
    }
})
