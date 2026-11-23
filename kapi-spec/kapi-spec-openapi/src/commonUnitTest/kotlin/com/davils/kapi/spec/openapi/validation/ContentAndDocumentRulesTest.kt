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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.head30
import com.davils.kapi.spec.openapi.fixture.head31
import com.davils.kapi.spec.openapi.fixture.reading
import com.davils.kapi.spec.openapi.fixture.reading31
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val COMPONENTS: String = "components:"

private const val TEXT: String = "{type: string}"

private val example = DiagnosticCodes.EXAMPLE_SHAPE

private val link = DiagnosticCodes.LINK_TARGET

private val media = DiagnosticCodes.MEDIA_TYPE_INVALID

private val missing = DiagnosticCodes.FIELD_MISSING

class ContentAndDocumentRulesTest : FunSpec({
    context("the servers") {
        test("declare every variable their address names") {
            val variables = "    variables: {region: {default: eu}}"
            val reading = reading31("servers:", "  - url: https://{region}.example.com/{stage}", variables)

            reading.codes() shouldBe listOf(DiagnosticCodes.SERVER_VARIABLE_UNDECLARED)
        }

        test("give a variable a default that is one of its values") {
            val variables = "    variables: {region: {default: asia, enum: [eu, us]}, stage: {default: live}}"
            val reading = reading31("servers:", "  - url: https://{region}.example.com/{stage}", variables)

            reading.codes() shouldBe listOf(DiagnosticCodes.SERVER_VARIABLE_DEFAULT)
        }
    }

    context("the tags") {
        test("are declared once") {
            reading31("tags: [{name: pets}, {name: toys}, {name: pets}]").codes() shouldBe
                listOf(DiagnosticCodes.TAG_DUPLICATE)
        }
    }

    context("a link") {
        test("names an operation that exists, by its id or by a reference") {
            val paths = "  /pets: {get: {operationId: list, responses: {'200': {description: Fine}}}}"
            val known = "    Known: {operationId: list}"
            val referred = "    Referred: {operationRef: '#/paths/~1pets/get'}"
            val unknown = "    Unknown: {operationId: gone}"
            val both = "    Both: {operationId: list, operationRef: '#/paths/~1pets/get'}"
            val neither = "    Neither: {description: Nothing}"
            val reading = reading31("paths:", paths, COMPONENTS, "  links:", known, referred, unknown, both, neither)

            reading.codes() shouldBe listOf(link, link, link)
        }
    }

    context("an example") {
        test("is given once") {
            val lines = listOf(
                COMPONENTS,
                "  examples:",
                "    Twice: {value: a, externalValue: https://example.com/a}",
                "  parameters:",
                "    Twice: {name: a, in: query, schema: $TEXT, example: a, examples: {first: {value: a}}}",
                "  headers:",
                "    Twice: {schema: $TEXT, example: a, examples: {first: {value: a}}}",
                "  requestBodies:",
                "    Twice: {content: {text/plain: {example: a, examples: {first: {value: a}}}}}",
            )

            reading31(lines).codes() shouldBe listOf(example, example, example, example)
        }
    }

    context("the key of a content") {
        test("is a media type") {
            val lines = listOf(
                COMPONENTS,
                "  parameters:",
                "    Odd: {name: a, in: query, content: {plain: {}}}",
                "  headers:",
                "    Odd: {content: {plain: {}}}",
                "  requestBodies:",
                "    Odd: {content: {plain: {}}}",
                "  responses:",
                "    Odd: {description: Odd, content: {plain: {}}}",
                "    Fine: {description: Fine, content: {'*/*': {}, 'text/*': {}, 'text/plain; charset=utf-8': {}}}",
            )

            reading31(lines).codes() shouldBe listOf(media, media, media, media)
        }
    }

    context("a document") {
        test("needs its paths in OpenAPI 3.0") {
            reading(head30.dropLast(1) + "components: {}").codes() shouldBe listOf(missing)
        }

        test("needs one of paths, components and webhooks in OpenAPI 3.1") {
            val webhook = listOf("webhooks:", "  changed: {post: {responses: {'204': {description: Received}}}}")

            reading(head31).codes() shouldBe listOf(missing)
            reading(head31 + "components: {}").diagnostics.shouldBeEmpty()
            reading(head31 + webhook).diagnostics.shouldBeEmpty()
        }
    }
})
