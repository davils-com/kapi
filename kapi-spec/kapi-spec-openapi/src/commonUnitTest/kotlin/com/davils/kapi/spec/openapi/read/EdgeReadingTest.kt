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

package com.davils.kapi.spec.openapi.read

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.errors
import com.davils.kapi.spec.openapi.fixture.head31
import com.davils.kapi.spec.openapi.fixture.operation
import com.davils.kapi.spec.openapi.fixture.parts
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.readingWith
import com.davils.kapi.spec.openapi.fixture.schema
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import com.davils.kapi.spec.openapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val COMPONENTS: String = "components:"

private const val SCHEMAS: String = "  schemas:"

private const val SUBJECT: String = "    Subject:"

private val mistyped = DiagnosticCodes.FIELD_TYPE

private val subject: List<String> = listOf(COMPONENTS, SCHEMAS, SUBJECT)

private val pets: List<String> = listOf("paths:", "  /pets:", "    get:", "      responses:", "        '200':")

class EdgeReadingTest : FunSpec({
    context("a header and a media type") {
        test("need no schema") {
            val plain = "            X-Plain: {content: {text/plain: {}}}"
            val content = listOf("          description: Pets", "          headers:", plain)
            val media = listOf("          content:", "            text/plain: {}")
            val reading = reading31(pets + content + media)
            val document = reading.document()
            val responses = document.operation("/pets", Method.Get).responses
            val response = document.resolve(responses.entries.values.single()).shouldNotBeNull()
            val header = document.resolve(response.headers.getValue("X-Plain")).shouldNotBeNull()

            reading.diagnostics.shouldBeEmpty()
            header.schema shouldBe null
            document.resolve(response.content.getValue("text/plain"))?.schema shouldBe null
        }
    }

    context("a response that refers to nothing readable") {
        test("is left out of the operation") {
            val reading = reading31(pets + "          \$ref: 1")

            reading.errors() shouldBe listOf(DiagnosticCodes.REFERENCE_SYNTAX)
        }
    }

    context("an oauth2 scheme") {
        test("may declare a single flow") {
            val scheme = listOf("  securitySchemes:", "    oauth:", "      type: oauth2", "      flows:")
            val address = "          authorizationUrl: https://example.com"
            val flow = listOf("        implicit:", address, "          scopes: {}")
            val reading = reading31(listOf(COMPONENTS) + scheme + flow)
            val document = reading.document()
            val oauth = document.resolve(document.parts().securitySchemes.getValue("oauth"))
            val flows = oauth.shouldBeInstanceOf<OAuth2Scheme>().flows

            flows.implicit.shouldNotBeNull()
            flows.password shouldBe null
            flows.clientCredentials shouldBe null
            flows.authorizationCode shouldBe null
        }

        test("names its scopes with texts") {
            val scheme = listOf("  securitySchemes:", "    oauth:", "      type: oauth2", "      flows:")
            val flow = listOf("        password:", "          tokenUrl: https://example.com", "          scopes:")
            val reading = reading31(listOf(COMPONENTS) + scheme + flow + "            read: 1")

            reading.codes() shouldBe listOf(mistyped)
        }
    }

    context("a discriminator") {
        test("maps its values to texts") {
            val discriminator = listOf("      discriminator:", "        propertyName: kind", "        mapping:")
            val reading = reading31(subject + discriminator + "          cat: 1")

            reading.codes() shouldBe listOf(mistyped)
        }
    }

    context("a count and a number") {
        test("are refused when a count is larger than any size") {
            reading31(subject + "      minLength: 99999999999").codes() shouldBe listOf(mistyped)
        }

        test("are refused when a number is no scalar") {
            reading31(subject + "      maximum: {}").codes() shouldBe listOf(mistyped)
        }
    }

    context("a value with a tag of its own") {
        test("is refused, because JSON has no such value") {
            val reading = reading31("x-photo: !!binary aGVsbG8=")

            reading.diagnostics.any { diagnostic -> diagnostic.isError } shouldBe true
        }
    }

    context("a reference without a fragment") {
        test("points at the whole of the other document") {
            val pet = DocumentUri("memory:/api/pet.yaml")
            val lines = head31 + subject + "      \$ref: pet.yaml"
            val reading = readingWith(mapOf(pet to yamlOf("type: string")), lines)
            val document = reading.document()
            val reference = document.schema("Subject").reference.shouldNotBeNull()
            val target = document.resolve(reference).shouldBeInstanceOf<SchemaDefinition>()

            reference.target.pointer.isRoot shouldBe true
            target.types shouldBe setOf(SchemaType.String)
        }
    }

    context("a document whose own address is no uri") {
        test("cannot resolve a relative reference") {
            val lines = head31 + subject + "      \$ref: pet.yaml"
            val source = Source.Text(yamlOf(lines), DocumentUri("urn:example:api"))
            val reading = OpenApi.reader().read(source)

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_NO_BASE)
        }
    }
})
