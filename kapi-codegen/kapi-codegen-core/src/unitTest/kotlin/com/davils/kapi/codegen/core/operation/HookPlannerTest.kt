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


package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.fixture.CALLBACKS_SENDER
import com.davils.kapi.codegen.core.fixture.WEBHOOKS_RECEIVER
import com.davils.kapi.codegen.core.fixture.WEBHOOKS_ROUTES
import com.davils.kapi.codegen.core.fixture.WEBHOOKS_SENDER
import com.davils.kapi.codegen.core.fixture.callback
import com.davils.kapi.codegen.core.fixture.codes
import com.davils.kapi.codegen.core.fixture.hookFileOf
import com.davils.kapi.codegen.core.fixture.hookFilesOf
import com.davils.kapi.codegen.core.fixture.hookGenerationOf
import com.davils.kapi.codegen.core.fixture.pet
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.fixture.webhook
import com.davils.kapi.codegen.core.options.Artifact
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val ADOPTED: String = "petAdopted"
private const val HOOKS: String = "webhook/"
private const val NAMED: String = "operationId: petAdopted"
private const val PET_BODY: String =
    "requestBody: {required: true, content: {application/json: {schema: {\$ref: '#/components/schemas/Pet'}}}}"
private const val KEY: String = "key: {type: apiKey, name: X-Hook-Key, in: header}"
private const val UNSUPPORTED: String = "The generator has no Kotlin for"

private val keyed: List<String> = listOf("  securitySchemes:", "    $KEY")

private val models: Set<Artifact> = setOf(Artifact.Models)

class HookPlannerTest : FunSpec({
    context("the webhooks of a document") {
        test("become a sender for whoever provides the api and a receiver with routes for whoever consumes it") {
            val senders = hookFilesOf(webhook(ADOPTED, NAMED), models + Artifact.Senders)
            val receivers = hookFilesOf(webhook(ADOPTED, NAMED), models + Artifact.Receivers)

            senders shouldContainAll listOf("webhook/$WEBHOOKS_SENDER.kt", "operation/PetstorePetAdoptedResult.kt")
            receivers shouldContainAll listOf(
                "webhook/$WEBHOOKS_RECEIVER.kt",
                "webhook/$WEBHOOKS_ROUTES.kt",
                "operation/PetstorePetAdoptedResponse.kt",
            )
            senders.none { path -> "Receiver" in path || "Routes" in path } shouldBe true
            receivers.none { path -> "Sender" in path } shouldBe true
        }

        test("are left out of an api that asks for neither side, and of its clients and servers") {
            val artifacts = models + Artifact.Client + Artifact.Server
            val files = hookFilesOf(webhook(ADOPTED, NAMED), artifacts)

            files.none { path -> HOOKS in path || "PetAdopted" in path } shouldBe true
        }

        test("are named after their operation, or after their key and their method") {
            hookFileOf(webhook(ADOPTED, NAMED), WEBHOOKS_SENDER) shouldContain "public suspend fun petAdopted("
            hookFileOf(webhook("pet-lost"), WEBHOOKS_SENDER) shouldContain "public suspend fun petLostPost("
        }

        test("are sent to the destination the caller names, which comes first") {
            val sender = hookFileOf(
                webhook(ADOPTED, NAMED, PET_BODY) + schemas(pet("name: {type: string}")),
                WEBHOOKS_SENDER,
            )

            sender shouldContain "destination: String,"
            sender shouldContain "destination(destination)"
            sender shouldNotContain "path("
        }

        test("are received at the path of their key and marked as deliveries") {
            val routes = hookFileOf(webhook("pet-lost"), WEBHOOKS_ROUTES)

            routes shouldContain "path = \"/petLost\","
            routes shouldContain "isDelivery = true,"
        }

        test("carry the model as the provider knows it, with what only it may set") {
            val lines = webhook(ADOPTED, NAMED, PET_BODY) +
                schemas(pet("id: {type: string, readOnly: true}", "name: {type: string}"))

            hookFileOf(lines, WEBHOOKS_RECEIVER) shouldContain "petAdopted(body: PetstorePet):"
        }

        test("that are marked to be ignored are left out") {
            val files = hookFilesOf(
                webhook(ADOPTED, NAMED, "x-kapi-ignore: true"),
                Artifact.entries.toSet() - Artifact.Mocks,
            )

            files.none { path -> HOOKS in path } shouldBe true
        }
    }

    context("the security of a delivery") {
        test("is its own and never the one of the document") {
            val lines = listOf("security: [{key: []}]") + webhook(ADOPTED, NAMED) + listOf("components:") + keyed
            val routes = hookFileOf(lines, WEBHOOKS_ROUTES)

            routes shouldContain "security = listOf(),"
        }

        test("names the scheme the receiver authenticates and the sender carries a credential for") {
            val lines = webhook(ADOPTED, NAMED, "security: [{key: []}]") + listOf("components:") + keyed

            hookFileOf(lines, WEBHOOKS_ROUTES) shouldContain "CredentialSource.Header(\"X-Hook-Key\")"
            hookFileOf(lines, WEBHOOKS_SENDER) shouldContain "CredentialPlacement.Header(\"X-Hook-Key\")"
        }
    }

    context("the callbacks of an operation") {
        test("become one sender and one receiver, named after the operation, the callback and the method") {
            val sender = hookFileOf(callback(), CALLBACKS_SENDER)

            sender shouldContain "public suspend fun subscribePetChangedPost("
            hookFileOf(callback(), "PetstoreCallbacksRoutes") shouldContain "path = \"/subscribePetChanged\","
        }

        test("keep the name their operation gives them") {
            hookFileOf(callback("operationId: petChanged"), CALLBACKS_SENDER) shouldContain "fun petChanged("
        }

        test("are refused when one callback has more than one expression") {
            val generation = hookGenerationOf(
                callback(expressions = "'{\$request.body#/url}' '{\$request.body#/other}'"),
            )

            generation.codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
            generation.diagnostics.single().message shouldContain "a callback with more than one expression"
        }

        test("of an operation that is ignored are left out with it") {
            val lines = listOf(
                "paths:",
                "  /subscriptions: {post: {x-kapi-ignore: true, responses: {'200': {description: ok}}, " +
                    "callbacks: {petChanged: {'{\$url}': {post: {responses: {'204': {description: taken}}}}}}}}",
            )

            hookFilesOf(lines, models + Artifact.Senders).none { path -> HOOKS in path } shouldBe true
        }
    }

    context("a delivery the generator cannot write") {
        test("is refused for a path parameter, which a destination has no place for") {
            val parameters = "parameters: [{name: id, in: path, required: true, schema: {type: string}}]"
            val generation = hookGenerationOf(webhook(ADOPTED, NAMED, parameters))

            generation.diagnostics.map { diagnostic -> diagnostic.message } shouldBe
                listOf("$UNSUPPORTED the path parameter id of a webhook or a callback yet")
        }

        test("is refused for pagination and for a response that is streamed") {
            val paged = "x-kapi-pagination: {style: page, parameter: page}"
            val page = "parameters: [{name: page, in: query, schema: {type: integer}}]"
            val items = "responses: {'200': {description: ok, content: {application/json: {schema: {type: array, " +
                "items: {type: string}}}}}}"
            val stream = "responses: {'200': {description: ok, content: {text/event-stream: {schema: {type: string}}}}}"
            val pagedHook = listOf("webhooks:", "  $ADOPTED: {post: {$NAMED, $paged, $page, $items}}")
            val streamedHook = listOf("webhooks:", "  $ADOPTED: {post: {$NAMED, $stream}}")
            val senders = models + Artifact.Senders

            hookGenerationOf(pagedHook, senders).diagnostics.single().message shouldBe
                "$UNSUPPORTED pagination on a webhook or a callback yet"
            hookGenerationOf(streamedHook, senders).diagnostics.single().message shouldBe
                "$UNSUPPORTED a streamed response of a webhook or a callback yet"
        }

        test("is refused for a parameter that would be named like the destination") {
            val parameters = "parameters: [{name: destination, in: query, schema: {type: string}}]"
            val generation = hookGenerationOf(webhook(ADOPTED, NAMED, parameters))

            generation.codes() shouldBe listOf(CodegenCodes.NAME_COLLISION)
            generation.diagnostics.single().message shouldContain "which the generated sender uses itself"
        }

        test("is refused on the receiving side for what a server does not serve yet") {
            val form = "requestBody: {content: {application/x-www-form-urlencoded: {schema: {type: object, " +
                "properties: {name: {type: string}}}}}}"
            val senders = hookGenerationOf(webhook(ADOPTED, NAMED, form), models + Artifact.Senders)
            val receivers = hookGenerationOf(webhook(ADOPTED, NAMED, form), models + Artifact.Receivers)

            senders.codes() shouldBe emptyList()
            receivers.codes() shouldBe listOf(CodegenCodes.OPERATION_UNSUPPORTED)
        }
    }
})
