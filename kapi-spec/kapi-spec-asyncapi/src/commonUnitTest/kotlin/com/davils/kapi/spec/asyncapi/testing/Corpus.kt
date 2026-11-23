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

package com.davils.kapi.spec.asyncapi.testing

import com.davils.kapi.spec.asyncapi.fixture.apiUri
import com.davils.kapi.spec.core.source.Source

private val chat: String =
    """
        |asyncapi: 3.0.0
        |id: 'urn:example:chat'
        |info:
        |  title: Chat
        |  version: 1.2.0
        |  description: A chat over WebSocket.
        |  contact:
        |    name: Davils
        |    url: https://example.com
        |    email: chat@example.com
        |  license:
        |    name: Apache-2.0
        |    url: https://www.apache.org/licenses/LICENSE-2.0
        |  tags:
        |    - name: chat
        |defaultContentType: application/json
        |servers:
        |  production:
        |    host: chat.example.com
        |    protocol: wss
        |    pathname: /v1
        |    security:
        |      - ${'$'}ref: '#/components/securitySchemes/token'
        |    bindings:
        |      ws: {}
        |channels:
        |  room:
        |    address: /rooms/{roomId}
        |    parameters:
        |      roomId:
        |        description: The id of the room
        |        examples: [general]
        |    servers:
        |      - ${'$'}ref: '#/servers/production'
        |    messages:
        |      chatMessage:
        |        ${'$'}ref: '#/components/messages/ChatMessage'
        |      typing:
        |        ${'$'}ref: '#/components/messages/Typing'
        |    bindings:
        |      ws:
        |        method: GET
        |        query:
        |          type: object
        |          properties:
        |            token:
        |              type: string
        |        headers:
        |          type: object
        |          properties:
        |            Authorization:
        |              type: string
        |        bindingVersion: 0.1.0
        |operations:
        |  sendMessage:
        |    action: send
        |    channel:
        |      ${'$'}ref: '#/channels/room'
        |    messages:
        |      - ${'$'}ref: '#/channels/room/messages/chatMessage'
        |      - ${'$'}ref: '#/channels/room/messages/typing'
        |    bindings:
        |      ws: {}
        |  receiveMessage:
        |    action: receive
        |    channel:
        |      ${'$'}ref: '#/channels/room'
        |    messages:
        |      - ${'$'}ref: '#/channels/room/messages/chatMessage'
        |components:
        |  securitySchemes:
        |    token:
        |      type: httpApiKey
        |      name: token
        |      in: query
        |  messages:
        |    ChatMessage:
        |      name: chatMessage
        |      payload:
        |        ${'$'}ref: '#/components/schemas/ChatMessage'
        |      correlationId:
        |        location: ${'$'}message.payload#/id
        |      bindings:
        |        ws: {}
        |      examples:
        |        - payload:
        |            id: '1'
        |            text: Hello
        |    Typing:
        |      payload:
        |        type: object
        |        properties:
        |          user:
        |            type: string
        |  schemas:
        |    ChatMessage:
        |      type: object
        |      required: [id, text]
        |      properties:
        |        id:
        |          type: string
        |        text:
        |          type: string
        |        author:
        |          ${'$'}ref: '#/components/schemas/User'
        |    User:
        |      type: object
        |      properties:
        |        name:
        |          type: string
    """.trimMargin()

private val events: String =
    """
        |asyncapi: 3.0.0
        |info:
        |  title: Account Events
        |  version: 2.0.0
        |servers:
        |  broker:
        |    host: 'kafka-{region}.example.com:9092'
        |    protocol: kafka
        |    protocolVersion: '3.6'
        |    variables:
        |      region:
        |        enum: [eu, us]
        |        default: eu
        |    security:
        |      - ${'$'}ref: '#/components/securitySchemes/sasl'
        |    bindings:
        |      kafka:
        |        schemaRegistryUrl: https://registry.example.com
        |        bindingVersion: 0.5.0
        |channels:
        |  userSignedUp:
        |    address: users.signedup
        |    messages:
        |      userSignedUp:
        |        ${'$'}ref: '#/components/messages/UserSignedUp'
        |    bindings:
        |      kafka:
        |        topic: users.signedup
        |        partitions: 10
        |        replicas: 3
        |operations:
        |  publishUserSignedUp:
        |    action: send
        |    channel:
        |      ${'$'}ref: '#/channels/userSignedUp'
        |    traits:
        |      - ${'$'}ref: '#/components/operationTraits/kafka'
        |    messages:
        |      - ${'$'}ref: '#/channels/userSignedUp/messages/userSignedUp'
        |components:
        |  securitySchemes:
        |    sasl:
        |      type: scramSha512
        |      description: SASL with SCRAM
        |  operationTraits:
        |    kafka:
        |      bindings:
        |        kafka:
        |          clientId:
        |            type: string
        |            enum: [accounts]
        |  messageTraits:
        |    commonHeaders:
        |      headers:
        |        type: object
        |        properties:
        |          traceId:
        |            type: string
        |  messages:
        |    UserSignedUp:
        |      name: userSignedUp
        |      contentType: application/vnd.apache.avro+json
        |      traits:
        |        - ${'$'}ref: '#/components/messageTraits/commonHeaders'
        |      payload:
        |        schemaFormat: application/vnd.apache.avro+json;version=1.9.0
        |        schema:
        |          type: record
        |          name: UserSignedUp
        |          fields:
        |            - name: userId
        |              type: string
        |            - name: email
        |              type: string
        |      bindings:
        |        kafka:
        |          key:
        |            type: string
    """.trimMargin()

private val requestReply: String =
    """
        |asyncapi: 3.0.0
        |info:
        |  title: Ping
        |  version: 1.0.0
        |servers:
        |  rabbit:
        |    host: rabbitmq.example.com
        |    protocol: amqp
        |    security:
        |      - ${'$'}ref: '#/components/securitySchemes/oauth'
        |channels:
        |  ping:
        |    address: ping
        |    messages:
        |      ping:
        |        ${'$'}ref: '#/components/messages/Ping'
        |  pong:
        |    address: null
        |    messages:
        |      pong:
        |        ${'$'}ref: '#/components/messages/Pong'
        |operations:
        |  pingRequest:
        |    action: send
        |    channel:
        |      ${'$'}ref: '#/channels/ping'
        |    messages:
        |      - ${'$'}ref: '#/channels/ping/messages/ping'
        |    reply:
        |      address:
        |        description: The queue the reply goes to
        |        location: ${'$'}message.header#/replyTo
        |      channel:
        |        ${'$'}ref: '#/channels/pong'
        |      messages:
        |        - ${'$'}ref: '#/channels/pong/messages/pong'
        |components:
        |  securitySchemes:
        |    oauth:
        |      type: oauth2
        |      flows:
        |        clientCredentials:
        |          tokenUrl: https://auth.example.com/token
        |          availableScopes:
        |            ping: Send pings
        |      scopes: [ping]
        |  messages:
        |    Ping:
        |      headers:
        |        type: object
        |        properties:
        |          replyTo:
        |            type: string
        |          correlationId:
        |            type: string
        |      correlationId:
        |        ${'$'}ref: '#/components/correlationIds/Request'
        |      payload:
        |        type: object
        |        properties:
        |          sentAt:
        |            type: string
        |            format: date-time
        |    Pong:
        |      correlationId:
        |        ${'$'}ref: '#/components/correlationIds/Request'
        |      payload:
        |        type: object
        |  correlationIds:
        |    Request:
        |      location: ${'$'}message.header#/correlationId
    """.trimMargin()

internal object Corpus {
    private val texts: Map<String, String> = mapOf(
        "chat" to chat,
        "events" to events,
        "request-reply" to requestReply,
    )

    val names: Set<String> get() = texts.keys

    fun text(name: String): String = requireNotNull(texts[name]) { "There is no document $name in the corpus" }

    fun source(name: String): Source = Source.Text(text(name), apiUri)
}
