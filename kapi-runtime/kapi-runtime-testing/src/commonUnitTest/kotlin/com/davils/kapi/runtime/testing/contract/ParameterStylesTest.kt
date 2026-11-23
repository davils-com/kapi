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

package com.davils.kapi.runtime.testing.contract

import com.davils.kapi.runtime.testing.fixture.documentOf
import com.davils.kapi.runtime.testing.mock.MockApi
import com.davils.kapi.runtime.testing.mock.mockApi
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header

private const val HOST: String = "https://api.example.com"

private const val OK: Int = 200

private const val INVALID: String = "parameter-invalid"

private val STYLES: String =
    """
        |openapi: 3.2.0
        |info:
        |  title: Styles
        |  version: 1.0.0
        |paths:
        |  /items/{label}/{matrix}/{simple}:
        |    parameters:
        |      - name: label
        |        in: path
        |        required: true
        |        style: label
        |        explode: true
        |        schema: { type: array, items: { type: integer } }
        |      - name: matrix
        |        in: path
        |        required: true
        |        style: matrix
        |        schema: { type: object, properties: { a: { type: integer } } }
        |      - name: simple
        |        in: path
        |        required: true
        |        explode: true
        |        schema: { type: object, properties: { x: { type: boolean } } }
        |    get:
        |      parameters:
        |        - name: space
        |          in: query
        |          style: spaceDelimited
        |          explode: false
        |          schema: { type: array, items: { type: number } }
        |        - name: pipe
        |          in: query
        |          style: pipeDelimited
        |          explode: false
        |          schema: { type: array, items: { type: integer } }
        |        - name: filter
        |          in: query
        |          style: deepObject
        |          explode: true
        |          schema: { type: object, properties: { max: { type: integer } } }
        |        - name: comma
        |          in: query
        |          explode: false
        |          schema: { type: object, properties: { k: { type: integer } } }
        |        - name: json
        |          in: query
        |          content:
        |            application/json:
        |              schema: { type: object, required: [a] }
        |        - name: text
        |          in: query
        |          content:
        |            text/plain:
        |              schema: { type: string, maxLength: 3 }
        |        - name: session
        |          in: cookie
        |          required: true
        |          schema: { type: integer }
        |        - name: X-Flags
        |          in: header
        |          schema: { type: array, items: { type: boolean } }
        |        - name: nothing
        |          in: query
        |          schema: { type: [integer, "null"] }
        |      responses:
        |        "200":
        |          description: Fine
        |  /tags/{scalar}/{matrixed}/{point}/{labels}:
        |    parameters:
        |      - name: point
        |        in: path
        |        required: true
        |        style: matrix
        |        schema: { type: integer }
        |      - name: labels
        |        in: path
        |        required: true
        |        style: label
        |        schema: { type: array, items: { type: integer } }
        |      - name: scalar
        |        in: path
        |        required: true
        |        style: label
        |        schema: { type: integer }
        |      - name: matrixed
        |        in: path
        |        required: true
        |        style: matrix
        |        explode: true
        |        schema: { type: array, items: { type: integer } }
        |    get:
        |      responses:
        |        "200":
        |          description: Fine
        |  /spread:
        |    get:
        |      parameters:
        |        - name: page
        |          in: query
        |          schema: { type: integer }
        |        - name: rest
        |          in: query
        |          schema: { type: object, additionalProperties: { type: integer } }
        |      responses:
        |        "200":
        |          description: Fine
        |  /raw:
        |    get:
        |      parameters:
        |        - name: q
        |          in: querystring
        |          content:
        |            application/x-www-form-urlencoded:
        |              schema: { type: string, minLength: 3 }
        |      responses:
        |        "200":
        |          description: Fine
    """.trimMargin()

private suspend fun findingsOf(vararg calls: Pair<String, Map<String, String>>): List<Pair<String, String>> {
    val api = mockApi {
        get("/items/{label}/{matrix}/{simple}") { respond(status = OK) }
        get("/tags/{scalar}/{matrixed}/{point}/{labels}") { respond(status = OK) }
        get("/spread") { respond(status = OK) }
        get("/raw") { respond(status = OK) }
    }
    api.call(calls.toList())
    val report = contractCheck(documentOf(STYLES)).check(api.exchanges)
    return report.findings.map { finding -> finding.subject to finding.code.value.substringAfterLast('.') }
}

private suspend fun MockApi.call(calls: List<Pair<String, Map<String, String>>>) {
    httpClient().use { http ->
        calls.forEach { (path, headers) ->
            http.get(HOST + path) { headers.forEach { (name, value) -> header(key = name, value = value) } }
        }
    }
}

class ParameterStylesTest : FunSpec({
    context("values in every style") {
        test("conform when each fits its schema") {
            val query = "space=1.5%202&pipe=1%7C2&filter%5Bmax%5D=3&comma=k,4&json=%7B%22a%22:1%7D&text=abc&nothing="
            val findings = findingsOf(
                "/items/.1.2.3/;matrix=a,5/x=true?$query" to mapOf("Cookie" to "session=7", "X-Flags" to "true,false"),
                "/tags/.4/;matrixed=1;matrixed=2/;point=3/.1,2" to emptyMap(),
                "/spread?page=1&a=2&b=3" to emptyMap(),
                "/raw?abc=def" to emptyMap(),
                "/raw" to emptyMap(),
            )

            findings shouldBe emptyList()
        }

        test("are reported where they break their schema") {
            val query = "space=x&pipe=1%7Cy&filter%5Bmax%5D=z&comma=k,v&json=%7B&text=long&nothing=n"
            val findings = findingsOf(
                "/items/.1.x/;matrix=a,b/x=maybe?$query" to mapOf("Cookie" to "other=1", "X-Flags" to "yes"),
                "/tags/.d/;matrixed=e/;point=p/.1,q" to emptyMap(),
                "/items/.1/;matrix=a,1,b/x=true?comma=k,4,z" to emptyMap(),
                "/spread?page=1&a=b" to emptyMap(),
                "/raw?a" to emptyMap(),
            )

            findings shouldBe listOf(
                "path parameter label" to INVALID,
                "path parameter matrix" to INVALID,
                "path parameter simple" to INVALID,
                "query parameter space" to INVALID,
                "query parameter pipe" to INVALID,
                "query parameter filter" to INVALID,
                "query parameter comma" to INVALID,
                "query parameter json" to INVALID,
                "query parameter text" to INVALID,
                "cookie parameter session" to "parameter-missing",
                "header parameter X-Flags" to INVALID,
                "query parameter nothing" to INVALID,
                "path parameter point" to INVALID,
                "path parameter labels" to INVALID,
                "path parameter scalar" to INVALID,
                "path parameter matrixed" to INVALID,
                "cookie parameter session" to "parameter-missing",
                "query parameter rest" to INVALID,
                "querystring parameter q" to INVALID,
            )
        }
    }
})
