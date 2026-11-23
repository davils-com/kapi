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

import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.DocumentBuilder
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Version
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.petstoreUri
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.testing.Corpus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val V31: String = "3.1.0"

private const val FILTER: String = "parameters/filter"

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
    "in: querystring",
    "style: cookie",
)

private val querystrings: List<String> = listOf(
    "openapi: 3.2.0",
    "info: {title: Search, version: 1.0.0}",
    "paths:",
    "  /items:",
    "    parameters:",
    "      - {name: session, in: cookie, style: cookie, schema: {type: string}}",
    "    get:",
    "      parameters:",
    "        - {\$ref: '#/components/parameters/filter'}",
    "        - {\$ref: '#/components/parameters/page'}",
    "      responses: {'200': {description: The items}}",
    "components:",
    "  parameters:",
    "    filter:",
    "      name: filter",
    "      in: querystring",
    "      content: {application/x-www-form-urlencoded: {schema: {type: object}}}",
    "    page: {name: X-Page, in: header, schema: {type: integer}}",
)

private suspend fun searched(): Document {
    val reader = OpenApi.reader()
    val reading = reader.read(Source.Text(yamlOf(querystrings), petstoreUri))
    reading.diagnostics.shouldBeEmpty()
    return reading.document()
}

private fun Document.as31(): Document = asVersion(V31)

private fun Document.asVersion(literal: String): Document {
    val builder = DocumentBuilder()
    builder.version = checkNotNull(Version.parseOrNull(literal))
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
    builder.targets.putAll(targets)
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
            written shouldContain "in: cookie"
        }
    }

    context("a parameter in the query string, which OpenAPI 3.2 adds") {
        test("is left out of an older version together with every reference to it") {
            listOf(V31, "3.0.3").forEach { literal ->
                val written = OpenApi.write(searched().asVersion(literal))

                written shouldNotContain "querystring"
                written shouldNotContain FILTER
                written shouldContain "parameters/page"
                written shouldContain "name: session"
            }
        }

        test("reads back as an older version without findings") {
            val text = OpenApi.write(searched().asVersion(V31))
            val reading = OpenApi.reader().read(Source.Text(text, petstoreUri))

            reading.diagnostics.shouldBeEmpty()
        }

        test("is kept by OpenAPI 3.2") {
            val written = OpenApi.write(searched())

            written shouldContain "in: querystring"
            written shouldContain "style: cookie"
            written shouldContain FILTER
        }
    }
})
