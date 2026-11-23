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

package com.davils.kapi.spec.asyncapi.fixture

internal val everything: String =
    """
        |asyncapi: 3.0.0
        |id: 'urn:example:everything'
        |x-owner: platform
        |info:
        |  title: Everything
        |  version: 1.0.0
        |  description: A document that uses every object of AsyncAPI 3.0.
        |  termsOfService: https://example.com/terms
        |  contact:
        |    name: Davils
        |    url: https://example.com
        |    email: api@example.com
        |    x-team: platform
        |  license:
        |    name: Apache-2.0
        |    url: https://www.apache.org/licenses/LICENSE-2.0
        |  tags:
        |    - name: everything
        |      description: Every object
        |      externalDocs:
        |        url: https://example.com/tags
        |    - ${'$'}ref: '#/components/tags/shared'
        |  externalDocs:
        |    description: More
        |    url: https://example.com/docs
        |defaultContentType: application/json
        |servers:
        |  production:
        |    host: '{region}.example.com:443'
        |    protocol: wss
        |    protocolVersion: '13'
        |    pathname: /api/{stage}
        |    description: The production server
        |    title: Production
        |    summary: Where it runs
        |    variables:
        |      region:
        |        enum: [eu, us]
        |        default: eu
        |        description: The region
        |        examples: [eu]
        |      stage:
        |        ${'$'}ref: '#/components/serverVariables/stage'
        |    security:
        |      - ${'$'}ref: '#/components/securitySchemes/user'
        |      - type: X509
        |        description: A certificate
        |    tags:
        |      - name: production
        |    externalDocs:
        |      ${'$'}ref: '#/components/externalDocs/docs'
        |    bindings:
        |      ws: {}
        |      http:
        |        bindingVersion: 0.3.0
        |  shared:
        |    ${'$'}ref: '#/components/servers/shared'
        |channels:
        |  room:
        |    address: rooms.{roomId}
        |    title: Room
        |    summary: A room
        |    description: The messages of a room
        |    servers:
        |      - ${'$'}ref: '#/servers/production'
        |    parameters:
        |      roomId:
        |        enum: [general, random]
        |        default: general
        |        description: The room
        |        examples: [general]
        |        location: ${'$'}message.payload#/room
        |    messages:
        |      said:
        |        ${'$'}ref: '#/components/messages/Said'
        |      left:
        |        payload:
        |          type: string
        |    tags:
        |      - name: rooms
        |    externalDocs:
        |      url: https://example.com/rooms
        |    bindings:
        |      ws:
        |        method: POST
        |        query:
        |          type: object
        |          properties:
        |            token:
        |              type: string
        |        headers:
        |          ${'$'}ref: '#/components/schemas/Headers'
        |        bindingVersion: 0.1.0
        |      kafka:
        |        topic: rooms
        |  shared:
        |    ${'$'}ref: '#/components/channels/shared'
        |operations:
        |  say:
        |    action: send
        |    channel:
        |      ${'$'}ref: '#/channels/room'
        |    title: Say
        |    summary: Say something
        |    description: Sends a message to the room
        |    security:
        |      - ${'$'}ref: '#/components/securitySchemes/oauth'
        |    tags:
        |      - name: speaking
        |    externalDocs:
        |      url: https://example.com/say
        |    bindings:
        |      ws: {}
        |      amqp:
        |        ack: true
        |    traits:
        |      - ${'$'}ref: '#/components/operationTraits/audited'
        |      - title: Inline trait
        |    messages:
        |      - ${'$'}ref: '#/channels/room/messages/said'
        |    reply:
        |      address:
        |        ${'$'}ref: '#/components/replyAddresses/back'
        |      channel:
        |        ${'$'}ref: '#/components/channels/replies'
        |      messages:
        |        - ${'$'}ref: '#/components/channels/replies/messages/ack'
        |  listen:
        |    ${'$'}ref: '#/components/operations/listen'
        |components:
        |  x-generated: false
        |  schemas:
        |    Said:
        |      type: object
        |      properties:
        |        text:
        |          type: string
        |    Headers:
        |      type: object
        |      properties:
        |        Authorization:
        |          type: string
        |    Avro:
        |      schemaFormat: application/vnd.apache.avro;version=1.9.0
        |      schema:
        |        type: record
        |        name: Said
        |        fields:
        |          - name: text
        |            type: string
        |    Proto:
        |      schemaFormat: application/vnd.google.protobuf;version=3
        |      schema: 'message Said { string text = 1; }'
        |  servers:
        |    shared:
        |      host: shared.example.com
        |      protocol: mqtt
        |  channels:
        |    shared:
        |      address: null
        |      messages:
        |        said:
        |          ${'$'}ref: '#/components/messages/Said'
        |    replies:
        |      messages:
        |        ack:
        |          ${'$'}ref: '#/components/messages/Ack'
        |  operations:
        |    listen:
        |      action: receive
        |      channel:
        |        ${'$'}ref: '#/channels/shared'
        |      reply:
        |        ${'$'}ref: '#/components/replies/none'
        |  messages:
        |    Said:
        |      headers:
        |        type: object
        |        properties:
        |          traceId:
        |            type: string
        |      payload:
        |        ${'$'}ref: '#/components/schemas/Said'
        |      correlationId:
        |        description: The trace
        |        location: ${'$'}message.header#/traceId
        |      contentType: application/json
        |      name: said
        |      title: Said
        |      summary: Something said
        |      description: What someone said
        |      tags:
        |        - name: talk
        |      externalDocs:
        |        url: https://example.com/said
        |      bindings:
        |        ws: {}
        |        kafka:
        |          key:
        |            type: string
        |      examples:
        |        - headers:
        |            traceId: abc
        |          payload:
        |            text: Hello
        |          name: hello
        |          summary: A greeting
        |      traits:
        |        - ${'$'}ref: '#/components/messageTraits/traced'
        |    Ack:
        |      payload:
        |        type: boolean
        |      examples:
        |        - headers:
        |            id: ack
        |  securitySchemes:
        |    user:
        |      type: userPassword
        |      description: A user and a password
        |    key:
        |      type: apiKey
        |      in: user
        |      description: A key
        |    httpKey:
        |      type: httpApiKey
        |      name: X-Key
        |      in: header
        |    bearer:
        |      type: http
        |      scheme: bearer
        |      bearerFormat: JWT
        |    oauth:
        |      type: oauth2
        |      description: OAuth
        |      flows:
        |        implicit:
        |          authorizationUrl: https://example.com/authorize
        |          availableScopes:
        |            say: Say something
        |        password:
        |          tokenUrl: https://example.com/token
        |          availableScopes: {}
        |        clientCredentials:
        |          tokenUrl: https://example.com/token
        |          refreshUrl: https://example.com/refresh
        |          availableScopes: {}
        |        authorizationCode:
        |          authorizationUrl: https://example.com/authorize
        |          tokenUrl: https://example.com/token
        |          availableScopes:
        |            listen: Listen
        |      scopes: [say]
        |    implicitOnly:
        |      type: oauth2
        |      flows:
        |        implicit:
        |          authorizationUrl: https://example.com/authorize
        |          availableScopes: {}
        |    oidc:
        |      type: openIdConnect
        |      openIdConnectUrl: https://example.com/.well-known/openid-configuration
        |      scopes: [openid]
        |    certificate:
        |      type: X509
        |    symmetric:
        |      type: symmetricEncryption
        |    asymmetric:
        |      type: asymmetricEncryption
        |    plain:
        |      type: plain
        |    scram256:
        |      type: scramSha256
        |    scram512:
        |      type: scramSha512
        |    kerberos:
        |      type: gssapi
        |  serverVariables:
        |    stage:
        |      default: live
        |  parameters:
        |    userId:
        |      description: A user
        |  correlationIds:
        |    trace:
        |      location: ${'$'}message.header#/traceId
        |  replies:
        |    channelOnly:
        |      channel:
        |        ${'$'}ref: '#/components/channels/replies'
        |    none:
        |      address:
        |        location: ${'$'}message.header#/replyTo
        |  replyAddresses:
        |    back:
        |      description: Back to the sender
        |      location: ${'$'}message.header#/replyTo
        |  externalDocs:
        |    docs:
        |      url: https://example.com/shared
        |  tags:
        |    shared:
        |      name: shared
        |  operationTraits:
        |    audited:
        |      title: Audited
        |      summary: An audited operation
        |      description: Every call is audited
        |      security:
        |        - ${'$'}ref: '#/components/securitySchemes/bearer'
        |      tags:
        |        - name: audit
        |      externalDocs:
        |        url: https://example.com/audit
        |      bindings:
        |        ${'$'}ref: '#/components/operationBindings/audit'
        |  messageTraits:
        |    traced:
        |      headers:
        |        type: object
        |      correlationId:
        |        ${'$'}ref: '#/components/correlationIds/trace'
        |      contentType: application/json
        |      name: traced
        |      title: Traced
        |      summary: A traced message
        |      description: Carries a trace
        |      tags:
        |        - name: tracing
        |      externalDocs:
        |        url: https://example.com/trace
        |      bindings:
        |        ${'$'}ref: '#/components/messageBindings/traced'
        |      examples:
        |        - payload: {}
        |  serverBindings:
        |    plain:
        |      mqtt:
        |        clientId: chat
        |  channelBindings:
        |    bare:
        |      ws: {}
        |    plain:
        |      amqp:
        |        is: queue
        |  operationBindings:
        |    audit:
        |      kafka:
        |        groupId:
        |          type: string
        |  messageBindings:
        |    traced:
        |      http:
        |        statusCode: 200
    """.trimMargin()
