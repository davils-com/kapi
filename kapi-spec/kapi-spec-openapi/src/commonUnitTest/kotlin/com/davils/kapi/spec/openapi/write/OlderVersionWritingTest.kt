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

package com.davils.kapi.spec.openapi.write

import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.DocumentBuilder
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Version
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.testing.Corpus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private val fieldsOf32: List<String> = listOf(
    "\$self:",
    "summary: Catalog",
    "parent:",
    "kind: nav",
    "kind: badge",
    "name: production",
    "dataValue:",
    "serializedValue:",
    "itemSchema:",
    "prefixEncoding:",
    "itemEncoding:",
    "summary: Matching items",
    "summary: Copied",
    "defaultMapping:",
    "nodeType:",
    "mediaTypes:",
    "oauth2MetadataUrl:",
    "deviceAuthorization:",
    "deviceAuthorizationUrl:",
    "deprecated:",
    "additionalOperations:",
    "query:",
)

private fun Document.as31(): Document {
    val builder = DocumentBuilder()
    builder.version = checkNotNull(Version.parseOrNull("3.1.0"))
    builder.self = self
    builder.info = info
    builder.jsonSchemaDialect = jsonSchemaDialect
    builder.servers += servers
    builder.paths = paths
    builder.webhooks += webhooks
    builder.components = components
    builder.security += security
    builder.tags += tags
    builder.externalDocs = externalDocs
    return builder.build()
}

private suspend fun latest(): Document {
    val reader = OpenApi.reader()
    return reader.read(Corpus.openApi("openapi-32")).document()
}

class OlderVersionWritingTest : FunSpec({
    context("a document of OpenAPI 3.2 written as OpenAPI 3.1") {
        test("leaves out every field that OpenAPI 3.2 adds") {
            val written = OpenApi.write(latest().as31())

            written shouldContain "openapi: 3.1.0"
            fieldsOf32.forEach { field -> written shouldNotContain field }
        }

        test("keeps every field that OpenAPI 3.1 knows") {
            val written = OpenApi.write(latest().as31())

            written shouldContain "name: catalog"
            written shouldContain "propertyName: kind"
            written shouldContain "type: oauth2"
            written shouldContain "operationId: listItems"
        }
    }
})
