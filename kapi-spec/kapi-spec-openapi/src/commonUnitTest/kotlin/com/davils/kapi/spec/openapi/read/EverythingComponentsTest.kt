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

import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.readingOf
import com.davils.kapi.spec.openapi.fixture.schema
import com.davils.kapi.spec.openapi.model.component.Components
import com.davils.kapi.spec.openapi.model.schema.Bound
import com.davils.kapi.spec.openapi.model.schema.FixedSchema
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import com.davils.kapi.spec.openapi.model.security.ApiKeyPlacement
import com.davils.kapi.spec.openapi.model.security.ApiKeyScheme
import com.davils.kapi.spec.openapi.model.security.HttpScheme
import com.davils.kapi.spec.openapi.model.security.MutualTlsScheme
import com.davils.kapi.spec.openapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.openapi.model.security.OpenIdConnectScheme
import com.davils.kapi.spec.openapi.model.security.SecurityScheme
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PET: String = "Pet"

private const val EVERYTHING: String = "everything.yaml"

private const val NAME: String = "name"

private const val KIND: String = "kind"

private const val BEARER: String = "bearer"

private const val FIXED: String = "fixed"

private const val TOKEN_URL: String = "https://example.com/token"

private const val AUTHORIZATION_URL: String = "https://example.com/authorize"

private const val LONGEST_NAME: Int = 64

private const val OLDEST: Long = 30L

private const val MOST_PROPERTIES: Int = 10

private const val MOST_TAGS: Int = 5

private val nameOnly: List<String> = listOf(NAME)

private val text: Set<SchemaType> = setOf(SchemaType.String)

private suspend fun components(): Components {
    val document = readingOf(EVERYTHING).document()
    return document.components.shouldNotBeNull()
}

private suspend fun pet(): SchemaDefinition {
    val document = readingOf(EVERYTHING).document()
    return document.schema(PET)
}

private fun Schema?.definition(): SchemaDefinition = shouldBeInstanceOf<SchemaDefinition>()

private fun SchemaDefinition.property(name: String): SchemaDefinition = properties.getValue(name).definition()

private suspend inline fun <reified T : SecurityScheme> scheme(name: String): T {
    val document: Document = readingOf(EVERYTHING).document()
    val referable = document.components
        .shouldNotBeNull()
        .securitySchemes
        .getValue(name)
    return document.resolve(referable).shouldBeInstanceOf<T>()
}

class EverythingComponentsTest : FunSpec({
    context("the components") {
        test("keep every section and their extensions") {
            val components = components()

            components.extensions.names.single() shouldBe ExtensionName("x-generated")
            components.schemas.keys shouldBe setOf(PET)
            components.responses.keys shouldBe setOf("Problem")
            components.parameters.keys shouldBe setOf("PetId")
            components.examples.keys shouldBe setOf("Fields")
            components.requestBodies.keys shouldBe setOf(PET)
            components.headers.keys shouldBe setOf("Shared")
            components.securitySchemes.keys shouldBe setOf(BEARER, "key", "mtls", "oauth", "oidc")
            components.links.keys shouldBe setOf("Toys")
            components.callbacks.keys shouldBe setOf("Shared")
        }
    }

    context("a schema") {
        test("keeps its identity and its annotations") {
            val pet = pet()

            pet.id shouldBe "https://example.com/schemas/pet"
            pet.dialect shouldBe "https://json-schema.org/draft/2020-12/schema"
            pet.anchor shouldBe "pet"
            pet.dynamicAnchor shouldBe "node"
            pet.comment shouldBe "A pet"
            pet.definitions
                .getValue("Name")
                .definition()
                .types shouldBe text
            pet.title shouldBe PET
            pet.description shouldBe "A pet of the store"
            pet.types shouldBe setOf(SchemaType.Object, SchemaType.Null)
            pet.extensions.text(ExtensionName("x-kapi-name")).toString() shouldBe "Present(value=Animal)"
            pet.externalDocs?.url shouldBe "https://example.com/pet"
        }

        test("keeps the constraints of a string") {
            val name = pet().property(NAME)

            name.types shouldBe text
            name.minLength shouldBe 1
            name.maxLength shouldBe LONGEST_NAME
            name.pattern shouldBe "^[A-Za-z]+$"
            name.format shouldBe NAME
            name.default shouldBe Value.Text("Rex")
            name.examples shouldBe listOf(Value.Text("Rex"), Value.Text("Bello"))
            name.isReadOnly shouldBe true
            name.isWriteOnly shouldBe true
            name.isDeprecated shouldBe true
            name.contentEncoding shouldBe "base64"
            name.contentMediaType shouldBe "text/plain"
            name.contentSchema.definition().types shouldBe text
        }

        test("keeps the tighter of an inclusive and an exclusive bound") {
            val age = pet().property("age")

            age.minimum shouldBe Bound(NumberValue.of(0L))
            age.maximum shouldBe Bound(NumberValue.of(OLDEST), isExclusive = true)
            age.multipleOf shouldBe NumberValue.of(1L)
        }

        test("keeps the constraints of an array") {
            val tags = pet().property("tags")

            tags.items.definition().types shouldBe text
            tags.prefixItems
                .single()
                .definition()
                .types shouldBe text
            tags.contains.definition().types shouldBe text
            tags.minContains shouldBe 1
            tags.maxContains shouldBe 2
            tags.minItems shouldBe 0
            tags.maxItems shouldBe MOST_TAGS
            tags.hasUniqueItems shouldBe true
            tags.unevaluatedItems.shouldBeInstanceOf<FixedSchema>().isAccepting shouldBe false
        }

        test("keeps the constraints of an object") {
            val pet = pet()

            pet.required shouldBe nameOnly
            pet.minProperties shouldBe 1
            pet.maxProperties shouldBe MOST_PROPERTIES
            pet.properties.keys shouldBe setOf(NAME, "age", "tags", KIND, FIXED, "parent", "friend")
            pet.patternProperties.keys shouldBe setOf("^x-")
            pet.additionalProperties.shouldBeInstanceOf<FixedSchema>().isAccepting shouldBe false
            pet.propertyNames.definition().pattern shouldBe "^[a-z]+$"
            pet.dependentRequired shouldBe mapOf("age" to nameOnly)
            pet.dependentSchemas
                .getValue("age")
                .definition()
                .required shouldBe nameOnly
            pet.unevaluatedProperties.shouldBeInstanceOf<FixedSchema>()
        }

        test("keeps enumerations, constants and references") {
            val pet = pet()
            val document = readingOf(EVERYTHING).document()
            val friend = pet.property("friend").reference.shouldNotBeNull()

            pet.property(KIND).enumeration shouldBe listOf(Value.Text("cat"), Value.Text("dog"))
            pet.property(FIXED).constant shouldBe Value.Text("pet")
            pet.property(FIXED).enumeration shouldBe null
            pet.property("parent").dynamicReference shouldBe "#node"
            friend.text shouldBe "#/components/schemas/Pet"
            document.resolve(friend) shouldBe pet
        }

        test("keeps how it is composed") {
            val pet = pet()

            pet.allOf.size shouldBe 1
            pet.anyOf.size shouldBe 1
            pet.oneOf.size shouldBe 1
            pet.not.definition().types shouldBe text
            pet.condition.definition().required shouldBe listOf("age")
            pet.consequence.definition().required shouldBe nameOnly
            pet.alternative.definition().required shouldBe listOf(KIND)
        }

        test("keeps its discriminator and its xml") {
            val pet = pet()

            pet.discriminator?.propertyName shouldBe KIND
            pet.discriminator?.mapping shouldBe mapOf("cat" to "#/components/schemas/Pet")
            pet.xml?.name shouldBe "pet"
            pet.xml?.namespace shouldBe "https://example.com/pet"
            pet.xml?.prefix shouldBe "p"
            pet.xml?.isAttribute shouldBe true
            pet.xml?.isWrapped shouldBe true
        }
    }

    context("a security scheme") {
        test("is an http scheme") {
            val bearer = scheme<HttpScheme>(BEARER)

            bearer.scheme shouldBe BEARER
            bearer.bearerFormat shouldBe "JWT"
            bearer.description shouldBe "A token"
        }

        test("is an api key") {
            val key = scheme<ApiKeyScheme>("key")

            key.name shouldBe "X-Key"
            key.placement shouldBe ApiKeyPlacement.Header
            key.description shouldBe "A key"
        }

        test("is mutual tls") {
            scheme<MutualTlsScheme>("mtls").description shouldBe "A certificate"
        }

        test("is openid connect") {
            val oidc = scheme<OpenIdConnectScheme>("oidc")

            oidc.openIdConnectUrl shouldBe "https://example.com/.well-known/openid-configuration"
            oidc.description shouldBe "OpenID Connect"
        }

        test("is oauth2 with each of its flows") {
            val flows = scheme<OAuth2Scheme>("oauth").flows
            val code = flows.authorizationCode.shouldNotBeNull()

            flows.implicit?.authorizationUrl shouldBe AUTHORIZATION_URL
            flows.implicit?.scopes shouldBe mapOf("read" to "Read")
            flows.password?.tokenUrl shouldBe TOKEN_URL
            flows.clientCredentials?.tokenUrl shouldBe TOKEN_URL
            code.authorizationUrl shouldBe AUTHORIZATION_URL
            code.tokenUrl shouldBe TOKEN_URL
            code.refreshUrl shouldBe "https://example.com/refresh"
            code.scopes shouldBe mapOf("read" to "Read", "write" to "Write")
        }
    }
})
