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

package com.davils.kapi.spec.openapi.fixture

private val everything: String = """openapi: 3.1.0
info:
  title: Everything
  version: 1.0.0
  summary: Every object once
  description: A document that uses every object of OpenAPI 3.1.
  termsOfService: https://example.com/terms
  contact:
    name: Davils
    url: https://example.com
    email: api@example.com
    x-team: platform
  license:
    name: Apache-2.0
    identifier: Apache-2.0
jsonSchemaDialect: https://spec.openapis.org/oas/3.1/dialect/base
servers:
  - url: https://{region}.example.com
    description: The regional endpoint
    variables:
      region:
        default: eu
        enum: [eu, us]
        description: The region
tags:
  - name: pets
    description: Everything about pets
    externalDocs:
      url: https://example.com/pets
      description: More about pets
externalDocs:
  url: https://example.com/docs
security:
  - bearer: []
  - oauth: [read, write]
paths:
  x-owner: platform
  /pets/{petId}:
    summary: A pet
    description: One pet of the store
    servers:
      - url: https://pets.example.com
    parameters:
      - ${'$'}ref: '#/components/parameters/PetId'
    get:
      operationId: getPet
      summary: Get a pet
      description: Returns one pet
      tags: [pets]
      deprecated: true
      externalDocs:
        url: https://example.com/pets/get
      security: []
      servers:
        - url: https://read.example.com
      parameters:
        - name: fields
          in: query
          description: The fields to return
          required: false
          deprecated: true
          allowEmptyValue: true
          style: form
          explode: false
          allowReserved: true
          schema:
            type: array
            items:
              type: string
          examples:
            few:
              summary: A few fields
              description: Only the name
              value: [name]
            external:
              externalValue: https://example.com/examples/fields.json
            shared:
              ${'$'}ref: '#/components/examples/Fields'
        - name: filter
          in: query
          content:
            application/json:
              schema:
                type: object
        - name: sort
          in: query
          schema:
            type: string
          example: name
      responses:
        x-rate: limited
        '200':
          description: The pet
          headers:
            ETag:
              description: The revision
              required: true
              deprecated: true
              style: simple
              explode: false
              schema:
                type: string
              examples:
                first:
                  value: abc
            X-Plain:
              schema:
                type: string
              example: abc
            X-Shared:
              ${'$'}ref: '#/components/headers/Shared'
          content:
            application/json:
              schema:
                ${'$'}ref: '#/components/schemas/Pet'
              examples:
                rex:
                  value:
                    name: Rex
              encoding:
                photo:
                  contentType: image/png
                  style: form
                  explode: true
                  allowReserved: true
                  headers:
                    X-Size:
                      schema:
                        type: integer
            text/plain:
              example:
                name: Rex
          links:
            owner:
              operationId: putPet
              parameters:
                ownerId: ${'$'}response.body#/ownerId
              requestBody: none
              description: The owner of the pet
              server:
                url: https://owners.example.com
            other:
              operationRef: '#/paths/~1owners/get'
            shared:
              ${'$'}ref: '#/components/links/Toys'
        4XX:
          ${'$'}ref: '#/components/responses/Problem'
        default:
          description: Anything else
      callbacks:
        onChange:
          '{${'$'}request.body#/url}':
            post:
              responses:
                '204':
                  description: Received
        shared:
          ${'$'}ref: '#/components/callbacks/Shared'
    put:
      operationId: putPet
      requestBody:
        description: The pet
        required: true
        content:
          application/json:
            schema:
              ${'$'}ref: '#/components/schemas/Pet'
      responses:
        '204':
          description: Stored
    post:
      operationId: postPet
      requestBody:
        ${'$'}ref: '#/components/requestBodies/Pet'
      responses:
        '201':
          description: Created
    delete:
      operationId: deletePet
      responses:
        '204':
          description: Gone
    options:
      operationId: optionsPet
      responses:
        '204':
          description: Allowed
    head:
      operationId: headPet
      responses:
        '204':
          description: There
    patch:
      operationId: patchPet
      responses:
        '204':
          description: Changed
    trace:
      operationId: tracePet
      responses:
        '204':
          description: Traced
  /shared:
    ${'$'}ref: '#/components/pathItems/Shared'
webhooks:
  petChanged:
    post:
      operationId: petChanged
      responses:
        '204':
          description: Received
components:
  x-generated: false
  schemas:
    Pet:
      ${'$'}id: https://example.com/schemas/pet
      ${'$'}schema: https://json-schema.org/draft/2020-12/schema
      ${'$'}anchor: pet
      ${'$'}dynamicAnchor: node
      ${'$'}comment: A pet
      ${'$'}defs:
        Name:
          type: string
      title: Pet
      description: A pet of the store
      type: [object, 'null']
      required: [name]
      minProperties: 1
      maxProperties: 10
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 64
          pattern: '^[A-Za-z]+${'$'}'
          format: name
          default: Rex
          examples: [Rex]
          example: Bello
          readOnly: true
          writeOnly: true
          deprecated: true
          contentEncoding: base64
          contentMediaType: text/plain
          contentSchema:
            type: string
        age:
          type: integer
          minimum: 0
          exclusiveMinimum: -1
          maximum: 30
          exclusiveMaximum: 30
          multipleOf: 1
        tags:
          type: array
          items:
            type: string
          prefixItems:
            - type: string
          contains:
            type: string
          minContains: 1
          maxContains: 2
          minItems: 0
          maxItems: 5
          uniqueItems: true
          unevaluatedItems: false
        kind:
          enum: [cat, dog]
        fixed:
          const: pet
        parent:
          ${'$'}dynamicRef: '#node'
        friend:
          ${'$'}ref: '#/components/schemas/Pet'
      patternProperties:
        '^x-':
          type: string
      additionalProperties: false
      propertyNames:
        pattern: '^[a-z]+${'$'}'
      dependentRequired:
        age: [name]
      dependentSchemas:
        age:
          required: [name]
      unevaluatedProperties: false
      allOf:
        - type: object
      anyOf:
        - type: object
      oneOf:
        - type: object
      not:
        type: string
      if:
        required: [age]
      then:
        required: [name]
      else:
        required: [kind]
      discriminator:
        propertyName: kind
        mapping:
          cat: '#/components/schemas/Pet'
      xml:
        name: pet
        namespace: https://example.com/pet
        prefix: p
        attribute: true
        wrapped: true
      externalDocs:
        url: https://example.com/pet
      x-kapi-name: Animal
  responses:
    Problem:
      description: A problem
  parameters:
    PetId:
      name: petId
      in: path
      required: true
      schema:
        type: string
  examples:
    Fields:
      value: [name]
  requestBodies:
    Pet:
      content:
        application/json:
          schema:
            ${'$'}ref: '#/components/schemas/Pet'
  headers:
    Shared:
      schema:
        type: string
  securitySchemes:
    bearer:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: A token
    key:
      type: apiKey
      name: X-Key
      in: header
      description: A key
    mtls:
      type: mutualTLS
      description: A certificate
    oauth:
      type: oauth2
      description: OAuth
      flows:
        implicit:
          authorizationUrl: https://example.com/authorize
          scopes:
            read: Read
        password:
          tokenUrl: https://example.com/token
          scopes: {}
        clientCredentials:
          tokenUrl: https://example.com/token
          scopes: {}
        authorizationCode:
          authorizationUrl: https://example.com/authorize
          tokenUrl: https://example.com/token
          refreshUrl: https://example.com/refresh
          scopes:
            read: Read
            write: Write
    oidc:
      type: openIdConnect
      openIdConnectUrl: https://example.com/.well-known/openid-configuration
      description: OpenID Connect
  links:
    Toys:
      operationId: deletePet
  callbacks:
    Shared:
      '{${'$'}request.body#/url}':
        post:
          responses:
            '204':
              description: Received
  pathItems:
    Shared:
      get:
        operationId: sharedGet
        responses:
          '200':
            description: Shared
"""

internal object Resources {
    private val texts: Map<String, String> = mapOf(
        "everything.yaml" to everything,
    )

    fun text(name: String): String = requireNotNull(texts[name]) { "There is no resource $name" }
}
