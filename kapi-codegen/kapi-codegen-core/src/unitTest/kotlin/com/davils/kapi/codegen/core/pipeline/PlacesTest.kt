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

package com.davils.kapi.codegen.core.pipeline

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.codegen.core.fixture.generator
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.textOf
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PlacesTest : FunSpec({
    context("the place of a value") {
        test("is where it was read, or the document in memory for a value that was never read") {
            val read = Location(DocumentUri("file:///specs/a.yaml"), JsonPointer.of("a"))

            Value.Text("x", read).place shouldBe read
            Value.Text("x").place shouldBe Location(DocumentUri.MEMORY, JsonPointer.ROOT)
        }
    }

    context("a query string whose media type cannot be resolved") {
        test("is refused where the parameter is") {
            val lines = paths(
                "/pets: {get: {operationId: a, parameters: [{name: q, in: querystring, " +
                    "content: {application/json: {\$ref: '#/components/mediaTypes/none'}}}], " +
                    "responses: {'204': {description: ok}}}}",
            )
            val text = textOf(lines).replace(oldValue = "openapi: 3.1.0", newValue = "openapi: 3.2.0")
            val reading = OpenApi.reader().read(Source.Text(text, DocumentUri("file:///specs/petstore.yaml")))
            val document = reading.shouldBeInstanceOf<Reading.Invalid>().partial
            val generator = generator { artifacts = setOf(Artifact.Models, Artifact.Client) }

            val codes = generator.generate(document).diagnostics.map(Diagnostic::code)

            codes.map { code -> code.value } shouldBe listOf("codegen.operation.unsupported")
        }
    }
})
