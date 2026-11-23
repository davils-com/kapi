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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.VersionFamily
import com.davils.kapi.spec.asyncapi.fixture.document
import com.davils.kapi.spec.asyncapi.fixture.everything
import com.davils.kapi.spec.asyncapi.fixture.parts
import com.davils.kapi.spec.asyncapi.fixture.readingOfText
import com.davils.kapi.spec.asyncapi.fixture.rewritten
import com.davils.kapi.spec.asyncapi.model.binding.WebSocketMethod
import com.davils.kapi.spec.asyncapi.model.channel.Channel
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.operation.Operation
import com.davils.kapi.spec.asyncapi.model.operation.OperationAction
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.asyncapi.model.security.ApiKeyPlacement
import com.davils.kapi.spec.asyncapi.model.security.ApiKeyScheme
import com.davils.kapi.spec.asyncapi.model.security.HttpApiKeyPlacement
import com.davils.kapi.spec.asyncapi.model.security.HttpApiKeyScheme
import com.davils.kapi.spec.asyncapi.model.security.HttpScheme
import com.davils.kapi.spec.asyncapi.model.security.OAuth2Scheme
import com.davils.kapi.spec.asyncapi.model.security.OpenIdConnectScheme
import com.davils.kapi.spec.asyncapi.model.security.SecuritySchemeType
import com.davils.kapi.spec.asyncapi.model.security.SimpleScheme
import com.davils.kapi.spec.asyncapi.model.server.Server
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.Value
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val EXAMPLE: String = "https://example.com"

private const val REPLY_TO: String = "\$message.header#/replyTo"

private const val TRACE: String = "\$message.header#/traceId"

private const val STATUS: Int = 200

private const val JSON: String = "application/json"

private const val GENERAL: String = "general"

private const val KAFKA: String = "kafka"

private const val LISTEN: String = "listen"

private const val SHARED: String = "shared"

private const val TRACED: String = "traced"

private suspend fun read(): Document = readingOfText(everything).document()

private fun <T : Element> Referable<T>.inline(): T {
    val inline = this as? Referable.Inline<T>
    return inline.shouldNotBeNull().value
}

private fun Document.room(): Channel = channels.getValue("room").inline()

private fun Document.say(): Operation = operations.getValue("say").inline()

private fun Document.said(): Message {
    val said = parts().messages.getValue("Said")
    return said.inline()
}

private fun Document.production(): Server = servers.getValue("production").inline()

class EverythingTest : FunSpec({
    test("reads the document without a finding") {
        readingOfText(everything).diagnostics.shouldBeEmpty()
    }

    test("reads the version, the id, the default content type and the extensions") {
        val document = read()

        document.version.family shouldBe VersionFamily.V30
        document.version.literal shouldBe "3.0.0"
        document.id shouldBe "urn:example:everything"
        document.defaultContentType shouldBe JSON
        document.extensions[ExtensionName("x-owner")] shouldBe Value.Text("platform")
        document.parts().extensions[ExtensionName("x-generated")] shouldBe Value.Flag(false)
    }

    test("reads the info with its contact, license, tags and documentation") {
        val info = read().info
        val contact = info.contact.shouldNotBeNull()
        val license = info.license.shouldNotBeNull()
        val first = info.tags.first().inline()

        info.title shouldBe "Everything"
        info.version shouldBe "1.0.0"
        info.description shouldBe "A document that uses every object of AsyncAPI 3.0."
        info.termsOfService shouldBe "$EXAMPLE/terms"
        contact.name shouldBe "Davils"
        contact.url shouldBe EXAMPLE
        contact.email shouldBe "api@example.com"
        contact.extensions[ExtensionName("x-team")] shouldBe Value.Text("platform")
        license.name shouldBe "Apache-2.0"
        license.url shouldBe "https://www.apache.org/licenses/LICENSE-2.0"
        first.name shouldBe "everything"
        first.description shouldBe "Every object"
        first.externalDocs
            .shouldNotBeNull()
            .inline()
            .url shouldBe "$EXAMPLE/tags"
        info.tags.last().shouldBeInstanceOf<Referable.Referenced>()
        info.externalDocs
            .shouldNotBeNull()
            .inline()
            .description shouldBe "More"
    }

    test("reads a server with its variables, security, tags, documentation and bindings") {
        val document = read()
        val server = document.production()
        val region = server.variables.getValue("region").inline()
        val stage = document.resolve(server.variables.getValue("stage")).shouldNotBeNull()
        val last = server.security.last().inline()
        val certificate = last.shouldBeInstanceOf<SimpleScheme>()
        val bindings = server.bindings.shouldNotBeNull().inline()

        server.host shouldBe "{region}.example.com:443"
        server.protocol shouldBe "wss"
        server.protocolVersion shouldBe "13"
        server.pathname shouldBe "/api/{stage}"
        server.description shouldBe "The production server"
        server.title shouldBe "Production"
        server.summary shouldBe "Where it runs"
        region.enumeration shouldContainExactly listOf("eu", "us")
        region.default shouldBe "eu"
        region.description shouldBe "The region"
        region.examples shouldContainExactly listOf("eu")
        stage.default shouldBe "live"
        certificate.type shouldBe SecuritySchemeType.X509
        certificate.description shouldBe "A certificate"
        server.tags
            .single()
            .inline()
            .name shouldBe "production"
        document.resolve(server.externalDocs.shouldNotBeNull()).shouldNotBeNull().url shouldBe "$EXAMPLE/shared"
        bindings.hasWebSocket shouldBe true
        bindings.protocols.getValue("http") shouldBe Value.Mapping(mapOf("bindingVersion" to Value.Text("0.3.0")))
        document.resolve(document.servers.getValue(SHARED)).shouldNotBeNull().protocol shouldBe "mqtt"
    }

    test("reads a channel with its parameters, messages, servers, tags and bindings") {
        val document = read()
        val channel = document.room()
        val parameter = channel.parameters.getValue("roomId").inline()
        val left = channel.messages.getValue("left").inline()
        val binding = channel.bindings.shouldNotBeNull().inline()
        val webSocket = binding.webSocket.shouldNotBeNull()

        channel.address shouldBe "rooms.{roomId}"
        channel.title shouldBe "Room"
        channel.summary shouldBe "A room"
        channel.description shouldBe "The messages of a room"
        channel.servers.single().text shouldBe "#/servers/production"
        parameter.enumeration shouldContainExactly listOf(GENERAL, "random")
        parameter.default shouldBe GENERAL
        parameter.description shouldBe "The room"
        parameter.examples shouldContainExactly listOf(GENERAL)
        parameter.expression shouldBe "\$message.payload#/room"
        document.resolve(channel.messages.getValue("said")).shouldNotBeNull().name shouldBe "said"
        left.payload
            .shouldNotBeNull()
            .inline()
            .definition shouldBe Value.Mapping(mapOf("type" to Value.Text("string")))
        channel.tags
            .single()
            .inline()
            .name shouldBe "rooms"
        channel.externalDocs
            .shouldNotBeNull()
            .inline()
            .url shouldBe "$EXAMPLE/rooms"
        webSocket.method shouldBe WebSocketMethod.Post
        webSocket.query
            .shouldNotBeNull()
            .inline()
            .format shouldBe SchemaFormat.AsyncApi
        webSocket.headers.shouldNotBeNull().shouldBeInstanceOf<Referable.Referenced>()
        webSocket.bindingVersion shouldBe "0.1.0"
        binding.protocols.keys shouldContainExactly setOf(KAFKA)
        document.resolve(document.channels.getValue(SHARED)).shouldNotBeNull().address shouldBe null
    }

    test("reads an operation with its traits, messages, reply and bindings") {
        val document = read()
        val operation = document.say()
        val reply = operation.reply.shouldNotBeNull().inline()
        val address = document.resolve(reply.address.shouldNotBeNull()).shouldNotBeNull()
        val listen = document.resolve(document.operations.getValue(LISTEN)).shouldNotBeNull()

        operation.action shouldBe OperationAction.Send
        operation.channel.shouldNotBeNull().text shouldBe "#/channels/room"
        operation.title shouldBe "Say"
        operation.summary shouldBe "Say something"
        operation.description shouldBe "Sends a message to the room"
        document.resolve(operation.security.single()).shouldBeInstanceOf<OAuth2Scheme>()
        operation.tags
            .single()
            .inline()
            .name shouldBe "speaking"
        operation.externalDocs
            .shouldNotBeNull()
            .inline()
            .url shouldBe "$EXAMPLE/say"
        operation.bindings
            .shouldNotBeNull()
            .inline()
            .hasWebSocket shouldBe true
        operation.traits.size shouldBe 2
        operation.traits
            .last()
            .inline()
            .title shouldBe "Inline trait"
        operation.messages.single().text shouldBe "#/channels/room/messages/said"
        address.description shouldBe "Back to the sender"
        address.expression shouldBe REPLY_TO
        reply.channel.shouldNotBeNull().text shouldBe "#/components/channels/replies"
        reply.messages.single().text shouldBe "#/components/channels/replies/messages/ack"
        listen.action shouldBe OperationAction.Receive
        val none = document.resolve(listen.reply.shouldNotBeNull()).shouldNotBeNull()
        none.address.shouldNotBeNull()
    }

    test("reads a message with its schemas, correlation id, examples, traits and bindings") {
        val document = read()
        val message = document.said()
        val correlation = message.correlationId.shouldNotBeNull().inline()
        val example = message.examples.single()
        val trait = document.resolve(message.traits.single()).shouldNotBeNull()

        message.headers
            .shouldNotBeNull()
            .inline()
            .format shouldBe SchemaFormat.AsyncApi
        document.resolve(message.payload.shouldNotBeNull()).shouldNotBeNull().schemaFormat shouldBe null
        correlation.description shouldBe "The trace"
        correlation.expression shouldBe TRACE
        message.contentType shouldBe JSON
        message.name shouldBe "said"
        message.title shouldBe "Said"
        message.summary shouldBe "Something said"
        message.description shouldBe "What someone said"
        message.tags
            .single()
            .inline()
            .name shouldBe "talk"
        message.externalDocs
            .shouldNotBeNull()
            .inline()
            .url shouldBe "$EXAMPLE/said"
        message.bindings
            .shouldNotBeNull()
            .inline()
            .protocols shouldContainKey KAFKA
        example.headers shouldBe mapOf("traceId" to Value.Text("abc"))
        example.payload shouldBe Value.Mapping(mapOf("text" to Value.Text("Hello")))
        example.name shouldBe "hello"
        example.summary shouldBe "A greeting"
        trait.name shouldBe TRACED
        trait.title shouldBe "Traced"
        trait.summary shouldBe "A traced message"
        trait.description shouldBe "Carries a trace"
        trait.contentType shouldBe JSON
        trait.headers.shouldNotBeNull()
        document.resolve(trait.correlationId.shouldNotBeNull()).shouldNotBeNull().expression shouldBe TRACE
        trait.tags
            .single()
            .inline()
            .name shouldBe "tracing"
        trait.externalDocs
            .shouldNotBeNull()
            .inline()
            .url shouldBe "$EXAMPLE/trace"
        document.resolve(trait.bindings.shouldNotBeNull()).shouldNotBeNull().protocols shouldContainKey "http"
        trait.examples.single().payload shouldBe Value.Mapping(emptyMap())
    }

    test("reads the schemas of every format") {
        val schemas = read().parts().schemas
        val avro = schemas.getValue("Avro").inline()
        val proto = schemas.getValue("Proto").inline()

        schemas.getValue("Said").inline().format shouldBe SchemaFormat.AsyncApi
        avro.format shouldBe SchemaFormat.Avro
        avro.schemaFormat shouldBe "application/vnd.apache.avro;version=1.9.0"
        proto.format shouldBe SchemaFormat.Protobuf
        proto.definition shouldBe Value.Text("message Said { string text = 1; }")
    }

    test("reads every type of security scheme") {
        val parts = read().parts()
        val schemes = parts.securitySchemes.mapValues { (_, scheme) -> scheme.inline() }
        val key = schemes.getValue("key").shouldBeInstanceOf<ApiKeyScheme>()
        val httpKey = schemes.getValue("httpKey").shouldBeInstanceOf<HttpApiKeyScheme>()
        val bearer = schemes.getValue("bearer").shouldBeInstanceOf<HttpScheme>()
        val oauth = schemes.getValue("oauth").shouldBeInstanceOf<OAuth2Scheme>()
        val oidc = schemes.getValue("oidc").shouldBeInstanceOf<OpenIdConnectScheme>()
        val flows = oauth.flows

        schemes.values.map { scheme -> scheme.type }.toSet() shouldBe SecuritySchemeType.entries.toSet()
        schemes.getValue("user").description shouldBe "A user and a password"
        key.placement shouldBe ApiKeyPlacement.User
        key.description shouldBe "A key"
        httpKey.name shouldBe "X-Key"
        httpKey.placement shouldBe HttpApiKeyPlacement.Header
        bearer.scheme shouldBe "bearer"
        bearer.bearerFormat shouldBe "JWT"
        oauth.description shouldBe "OAuth"
        oauth.scopes shouldContainExactly listOf("say")
        flows.implicit.shouldNotBeNull().authorizationUrl shouldBe "$EXAMPLE/authorize"
        flows.password.shouldNotBeNull().tokenUrl shouldBe "$EXAMPLE/token"
        flows.clientCredentials.shouldNotBeNull().refreshUrl shouldBe "$EXAMPLE/refresh"
        flows.authorizationCode.shouldNotBeNull().availableScopes shouldBe mapOf(LISTEN to "Listen")
        oidc.openIdConnectUrl shouldBe "$EXAMPLE/.well-known/openid-configuration"
        oidc.scopes shouldContainExactly listOf("openid")
    }

    test("reads every map of the components") {
        val parts = read().parts()

        parts.servers.keys shouldContainExactly setOf(SHARED)
        parts.channels.keys shouldContainExactly setOf(SHARED, "replies")
        parts.operations.keys shouldContainExactly setOf(LISTEN)
        parts.messages.keys shouldContainExactly setOf("Said", "Ack")
        parts.serverVariables.keys shouldContainExactly setOf("stage")
        parts.parameters.keys shouldContainExactly setOf("userId")
        parts.correlationIds.keys shouldContainExactly setOf("trace")
        parts.replies.keys shouldContainExactly setOf("channelOnly", "none")
        parts.replyAddresses.keys shouldContainExactly setOf("back")
        parts.externalDocs.keys shouldContainExactly setOf("docs")
        parts.tags.keys shouldContainExactly setOf(SHARED)
        parts.operationTraits.keys shouldContainExactly setOf("audited")
        parts.messageTraits.keys shouldContainExactly setOf(TRACED)
        parts.serverBindings
            .getValue("plain")
            .inline()
            .protocols shouldContainKey "mqtt"
        parts.channelBindings
            .getValue("plain")
            .inline()
            .protocols shouldContainKey "amqp"
        parts.operationBindings
            .getValue("audit")
            .inline()
            .protocols shouldContainKey KAFKA
        val traced = parts.messageBindings.getValue(TRACED).inline()
        val http = traced.protocols.getValue("http")
        val status = http.shouldBeInstanceOf<Value.Mapping>()["statusCode"].shouldBeInstanceOf<Value.Number>()
        status.value.toIntOrNull() shouldBe STATUS
    }

    test("writes the document so that it reads back to the same document") {
        val document = read()

        document.rewritten() shouldBe document
    }
})
