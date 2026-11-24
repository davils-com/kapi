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

import com.davils.kapi.codegen.core.fixture.generator
import com.davils.kapi.codegen.core.fixture.paths
import com.davils.kapi.codegen.core.fixture.textOf
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PAGE: String = "content: {application/json: {schema: {type: object, " +
    "properties: {items: {type: array, items: {type: string}}, next: {type: string}}}}}"

private const val TEXT: String = "{type: string}"

private const val NONE: String = "responses: {'204': {description: ok}}"

private const val GET: String = "/pets: {get: {operationId: a"

private const val POST: String = "/pets: {post: {operationId: a"

private const val GET_AT: String = "@/paths/~1pets/get"

private const val PAGINATION: String = "codegen.extension.value$GET_AT/x-kapi-pagination"

private val cases: Map<String, Pair<List<String>, List<String>>> = mapOf(
    "a pagination whose style is no text" to (
        paths("$GET, x-kapi-pagination: {style: 7}, responses: {'200': {description: ok, $PAGE}}}}") to
            listOf(PAGINATION)
        ),
    "a pagination of a style nobody knows" to (
        paths("$GET, x-kapi-pagination: {style: odd}, responses: {'200': {description: ok, $PAGE}}}}") to
            listOf(PAGINATION)
        ),
    "a pagination of an operation that answers no page" to (
        paths("$GET, x-kapi-pagination: {style: cursor}, $NONE}}") to
            listOf("codegen.pagination.invalid$GET_AT", "codegen.pagination.invalid$GET_AT")
        ),
    "a cursor pagination with its parameter, items and next" to (
        paths(
            "$GET, x-kapi-pagination: {style: cursor, parameter: c, items: items, next: next}, " +
                "parameters: [{name: c, in: query, schema: {type: string}}], " +
                "responses: {'200': {description: ok, $PAGE}}}}",
        ) to emptyList()
        ),
    "an extension on a header where it means nothing" to (
        paths(
            "$GET, responses: {'200': {description: ok, headers: {Content-Type: {schema: {type: string}}, " +
                "X-Skip: {x-kapi-ignore: false, schema: {type: string}}}}}}}",
        ) to listOf("codegen.extension.site$GET_AT/responses/200/headers/X-Skip/x-kapi-ignore")
        ),
    "a tri-state that is no flag" to (
        paths(
            "$POST, x-kapi-tri-state: 7, requestBody: {content: {application/json: {schema: {type: object, " +
                "properties: {a: {type: string}}}}}}, $NONE}}",
        ) to listOf("codegen.extension.value@/paths/~1pets/post/x-kapi-tri-state")
        ),
    "a tri-state on a body that is no object" to (
        paths("$POST, x-kapi-tri-state: true, requestBody: {content: {application/json: {schema: $TEXT}}}, $NONE}}") to
            listOf("codegen.operation.unsupported@/paths/~1pets/post")
        ),
    "a path and a cookie parameter in their default styles" to (
        paths(
            "/pets/{id}: {get: {operationId: a, parameters: [{name: id, in: path, required: true, " +
                "schema: {type: string}}, {name: s, in: cookie, schema: {type: string}}], $NONE}}",
        ) to emptyList()
        ),
    "a required query string of JSON" to (
        paths(
            "/pets: {query: {operationId: a, parameters: [{name: q, in: querystring, required: true, " +
                "content: {application/json: {schema: {type: string}}}}], $NONE}}",
        ) to emptyList()
        ),
    "a name that is no text" to (
        paths("$GET, x-kapi-name: 7, $NONE}}") to listOf("codegen.extension.value$GET_AT/x-kapi-name")
        ),
    "a body that is a map" to (
        paths(
            "$POST, requestBody: {content: {application/json: {schema: {type: object, " +
                "additionalProperties: {type: string}}}}}, $NONE}}",
        ) to emptyList()
        ),
)

class PlannerEdgesTest : FunSpec({
    context("an operation at the edge of what the generator supports") {
        cases.forEach { (name, case) ->
            val (lines, expected) = case

            test("with $name is generated or refused with every finding where it is") {
                val text = textOf(lines).replace(oldValue = "openapi: 3.1.0", newValue = "openapi: 3.2.0")
                val reading = OpenApi.reader().read(Source.Text(text, DocumentUri("file:///specs/petstore.yaml")))
                val read = reading.shouldBeInstanceOf<Reading.Read>()
                val generator = generator { artifacts = setOf(Artifact.Models, Artifact.Client, Artifact.Server) }
                val found = generator.generate(read.document, read.sources).diagnostics.map { diagnostic ->
                    diagnostic.code.value + "@" + diagnostic.location?.run { pointer.text }
                }

                found shouldBe expected
            }
        }
    }
})
