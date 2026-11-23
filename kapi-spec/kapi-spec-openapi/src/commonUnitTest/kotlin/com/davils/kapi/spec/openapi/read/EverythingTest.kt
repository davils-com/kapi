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

import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.operation
import com.davils.kapi.spec.openapi.fixture.parts
import com.davils.kapi.spec.openapi.fixture.readingOf
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.content.ResponseStatus
import com.davils.kapi.spec.openapi.model.parameter.Parameter
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.path.PathTemplate
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val EVERYTHING: String = "everything.yaml"

private const val JSON: String = "application/json"

private const val PET_PATH: String = "/pets/{petId}"

private const val SHARED: String = "shared"

private val pet: PathTemplate = PathTemplate(PET_PATH)

private val ok: ResponseStatus = ResponseStatus("200")

private suspend fun everything(): Document = readingOf(EVERYTHING).document()

private fun Document.item(): PathItem = paths.shouldNotBeNull().items.getValue(pet)

private fun Document.get(): Operation = item().operations.getValue(Method.Get)

private fun Document.okResponse(): Response {
    val referable = get().responses.entries.getValue(ok)
    return resolve(referable).shouldNotBeNull()
}

private fun Operation.inlineParameter(index: Int): Parameter {
    val inline = parameters[index].shouldBeInstanceOf<Referable.Inline<Parameter>>()
    return inline.value
}

class EverythingTest : FunSpec({
    context("the document that uses every object") {
        test("is read without a finding") {
            readingOf(EVERYTHING).diagnostics.shouldBeEmpty()
        }

        test("keeps its info, its contact and its license") {
            val info = everything().info
            val contact = info.contact.shouldNotBeNull()

            info.summary shouldBe "Every object once"
            info.description shouldBe "A document that uses every object of OpenAPI 3.1."
            info.termsOfService shouldBe "https://example.com/terms"
            contact.name shouldBe "Davils"
            contact.url shouldBe "https://example.com"
            contact.email shouldBe "api@example.com"
            contact.extensions[ExtensionName("x-team")] shouldBe Value.Text("platform")
            info.license?.name shouldBe "Apache-2.0"
            info.license?.identifier shouldBe "Apache-2.0"
            info.license?.url shouldBe null
        }

        test("keeps its dialect, its servers, its tags, its documentation and its security") {
            val document = everything()
            val server = document.servers.single()
            val region = server.variables.getValue("region")
            val tag = document.tags.single()

            document.jsonSchemaDialect shouldBe "https://spec.openapis.org/oas/3.1/dialect/base"
            server.url shouldBe "https://{region}.example.com"
            server.description shouldBe "The regional endpoint"
            region.default shouldBe "eu"
            region.enumeration shouldBe listOf("eu", "us")
            region.description shouldBe "The region"
            tag.name shouldBe "pets"
            tag.description shouldBe "Everything about pets"
            tag.externalDocs?.url shouldBe "https://example.com/pets"
            tag.externalDocs?.description shouldBe "More about pets"
            document.externalDocs?.url shouldBe "https://example.com/docs"
            document.security.map { requirement -> requirement.schemes } shouldBe
                listOf(mapOf("bearer" to emptyList()), mapOf("oauth" to listOf("read", "write")))
        }
    }

    context("a path item") {
        test("keeps what it says about every operation below it") {
            val document = everything()
            val item = document.item()
            val shared = document.resolve(item.parameters.single()).shouldNotBeNull()
            val declared = document.paths.shouldNotBeNull()

            declared.extensions.names shouldBe setOf(ExtensionName("x-owner"))
            item.summary shouldBe "A pet"
            item.description shouldBe "One pet of the store"
            item.servers.single().url shouldBe "https://pets.example.com"
            item.operations.keys shouldBe Method.entries.toSet() - Method.Query
            shared.name shouldBe "petId"
            shared.placement shouldBe ParameterPlacement.Path
            shared.isRequired shouldBe true
        }

        test("may refer to a shared path item") {
            val document = everything()
            val item = document.paths
                .shouldNotBeNull()
                .items
                .getValue(PathTemplate("/shared"))
            val shared = document.resolve(item.reference.shouldNotBeNull())

            shared.shouldBeInstanceOf<PathItem>().operations.keys shouldBe setOf(Method.Get)
            document.parts().pathItems.keys shouldBe setOf("Shared")
        }

        test("is read below the webhooks as well") {
            val webhook = everything().webhooks.getValue("petChanged")

            webhook.operations.getValue(Method.Post).operationId shouldBe "petChanged"
        }
    }

    context("an operation") {
        test("keeps its description, its tags, its servers and its own security") {
            val get = everything().get()

            get.operationId shouldBe "getPet"
            get.summary shouldBe "Get a pet"
            get.description shouldBe "Returns one pet"
            get.tags shouldBe listOf("pets")
            get.isDeprecated shouldBe true
            get.externalDocs?.url shouldBe "https://example.com/pets/get"
            get.security shouldBe emptyList()
            get.servers.single().url shouldBe "https://read.example.com"
        }

        test("inherits the security of the document when it names none") {
            val put = everything().operation(PET_PATH, Method.Put)

            put.security shouldBe null
        }

        test("keeps every trait of a parameter") {
            val document = everything()
            val fields = document.get().inlineParameter(0)
            val shared = document.resolve(fields.examples.getValue(SHARED)).shouldNotBeNull()

            fields.name shouldBe "fields"
            fields.placement shouldBe ParameterPlacement.Query
            fields.description shouldBe "The fields to return"
            fields.isRequired shouldBe false
            fields.isDeprecated shouldBe true
            fields.isEmptyValueAllowed shouldBe true
            fields.style shouldBe ParameterStyle.Form
            fields.isExploded shouldBe false
            fields.isReservedAllowed shouldBe true
            fields.examples.keys shouldBe setOf("few", "external", SHARED)
            shared.value shouldBe Value.Sequence(listOf(Value.Text("name")))
            document.get().inlineParameter(2).example shouldBe Value.Text("name")
        }

        test("keeps the examples and the content of a parameter") {
            val document = everything()
            val fields = document.get().inlineParameter(0)
            val few = document.resolve(fields.examples.getValue("few")).shouldNotBeNull()
            val external = document.resolve(fields.examples.getValue("external")).shouldNotBeNull()
            val filter = document.get().inlineParameter(1)

            few.summary shouldBe "A few fields"
            few.description shouldBe "Only the name"
            external.externalValue shouldBe "https://example.com/examples/fields.json"
            filter.style shouldBe null
            filter.isExploded shouldBe null
            filter.content.keys shouldBe setOf(JSON)
        }

        test("keeps its request body, inline or shared") {
            val document = everything()
            val operations = document.item().operations
            val inline = document.resolve(operations.getValue(Method.Put).requestBody.shouldNotBeNull())
            val shared = document.resolve(operations.getValue(Method.Post).requestBody.shouldNotBeNull())

            inline.shouldNotBeNull().description shouldBe "The pet"
            inline.isRequired shouldBe true
            inline.content.keys shouldBe setOf(JSON)
            shared.shouldNotBeNull().isRequired shouldBe false
        }

        test("keeps its callbacks, inline or shared") {
            val document = everything()
            val callbacks = document.get().callbacks
            val inline = document.resolve(callbacks.getValue("onChange")).shouldNotBeNull()
            val shared = document.resolve(callbacks.getValue(SHARED)).shouldNotBeNull()

            inline.expressions.keys.single() shouldBe "{\$request.body#/url}"
            shared.expressions shouldBe inline.expressions
        }
    }

    context("the responses of an operation") {
        test("are keyed by status and carry their extensions") {
            val document = everything()
            val responses = document.get().responses
            val problem = document.resolve(responses.entries.getValue(ResponseStatus("4XX")))

            responses.entries.keys.map { status -> status.value } shouldBe listOf("200", "4XX", "default")
            responses.extensions.names.single() shouldBe ExtensionName("x-rate")
            problem.shouldNotBeNull().description shouldBe "A problem"
            document.okResponse().description shouldBe "The pet"
        }

        test("keep their headers") {
            val document = everything()
            val headers = document.okResponse().headers
            val revision = document.resolve(headers.getValue("ETag")).shouldNotBeNull()
            val shared = document.resolve(headers.getValue("X-Shared")).shouldNotBeNull()

            revision.description shouldBe "The revision"
            revision.isRequired shouldBe true
            revision.isDeprecated shouldBe true
            revision.style shouldBe ParameterStyle.Simple
            revision.isExploded shouldBe false
            revision.examples.keys shouldBe setOf("first")
            revision.content shouldBe emptyMap()
            shared.schema.shouldNotBeNull()
            document.resolve(headers.getValue("X-Plain"))?.example shouldBe Value.Text("abc")
        }

        test("keep their content with examples and encodings") {
            val document = everything()
            val content = document.okResponse().content
            val media = document.resolve(content.getValue(JSON)).shouldNotBeNull()
            val photo = media.encoding.getValue("photo")
            val plain = document.resolve(content.getValue("text/plain")).shouldNotBeNull()

            media.schema.shouldNotBeNull()
            plain.example shouldBe Value.Mapping(mapOf("name" to Value.Text("Rex")))
            media.examples.keys shouldBe setOf("rex")
            photo.contentType shouldBe "image/png"
            photo.style shouldBe ParameterStyle.Form
            photo.isExploded shouldBe true
            photo.isReservedAllowed shouldBe true
            photo.headers.keys shouldBe setOf("X-Size")
        }

        test("keep their links") {
            val document = everything()
            val links = document.okResponse().links
            val owner = document.resolve(links.getValue("owner")).shouldNotBeNull()
            val other = document.resolve(links.getValue("other")).shouldNotBeNull()
            val shared = document.resolve(links.getValue(SHARED)).shouldNotBeNull()

            owner.operationId shouldBe "putPet"
            owner.parameters shouldBe mapOf("ownerId" to Value.Text("\$response.body#/ownerId"))
            owner.requestBody shouldBe Value.Text("none")
            owner.description shouldBe "The owner of the pet"
            owner.server?.url shouldBe "https://owners.example.com"
            other.operationReference shouldBe "#/paths/~1owners/get"
            shared.operationId shouldBe "deletePet"
        }
    }
})
