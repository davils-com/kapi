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

package com.davils.kapi.spec.testing

private val additionalProperties: String = """# A schema that names some of its properties and admits others.
#
# `properties` beside `additionalProperties` used to end generation with an
# `UnsupportedOperationException` saying kotlinx cannot represent it. There is no *automatic*
# representation - a bag of unknown values would have to be `Any?`, which nothing serialises - but a
# serialiser written by hand can do it, and its shape is entirely mechanical.

openapi: 3.0.3

info:
  title: open-objects
  version: 1.0.0

paths:
  /v1/records:
    post:
      operationId: putRecord
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/Record" }
      responses:
        "200":
          description: stored
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Record" }

components:
  schemas:
    # Named and unnamed at once.
    Record:
      type: object
      required: [id]
      additionalProperties: true
      properties:
        id: { type: string }
        label: { type: string }

    # Only unnamed, which is an ordinary map and needs none of this.
    Labels:
      type: object
      additionalProperties: { type: string }
"""

private val callbacksAndWebhooks: String = """# Operations that travel the other way.
#
# Everything else in this generator describes a request a caller sends and an answer it gets back.
# These two describe the reverse - the API calling the caller - and until now both produced nothing
# at all. A document declaring them generated as though it had not.
#
#   * A **callback** is declared on the operation that arms it, and its key is the runtime expression
#     saying where to send it. The generator cannot evaluate that expression, so the sender takes the
#     URL as a parameter and the receiver mounts wherever the application puts it.
#   * A **webhook** (3.1) is the same shape without the arming operation: the API sends it because
#     the consumer subscribed out of band.
#
# Neither names a path the generator could mount on, which is the whole reason they are not just more
# entries under `paths`.

openapi: 3.1.0

info:
  title: callbacks
  version: 1.0.0

paths:
  /v1/jobs:
    post:
      operationId: createJob
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/NewJob" }
      responses:
        "202":
          description: accepted
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Job" }
      callbacks:
        jobFinished:
          "{${'$'}request.body#/callbackUrl}":
            post:
              operationId: jobFinished
              requestBody:
                required: true
                content:
                  application/json:
                    schema: { ${'$'}ref: "#/components/schemas/JobResult" }
              responses:
                "204": { description: taken }
                "410":
                  description: gone, stop sending
                  content:
                    application/json:
                      schema: { ${'$'}ref: "#/components/schemas/Problem" }

webhooks:
  # A webhook with a body and nothing to say back.
  jobCancelled:
    post:
      operationId: jobCancelled
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/JobResult" }
      responses:
        "204": { description: taken }

components:
  schemas:
    NewJob:
      type: object
      required: [callbackUrl]
      properties:
        callbackUrl: { type: string }

    Job:
      type: object
      required: [id]
      properties:
        id: { type: string }

    JobResult:
      type: object
      required: [id, state]
      properties:
        id: { type: string }
        state: { type: string }

    Problem:
      type: object
      properties:
        title: { type: string }
"""

private val contentNegotiationRequest: String = """# A request body offered under more than one media type.
#
# Two different things hide behind one media-type list, and the generator treated them the same:
# it kept whichever the document happened to list first and dropped the rest.
#
#   * The same schema under several names is a choice of *encoding*. Kubernetes' patch operations
#     offer one `Patch` under five media types; Kratos offers a JSON Patch under two. A caller that
#     needed one of the others had no way to ask for it.
#   * Different schemas under different names are different calls. Collapsing those loses one of
#     them silently, which is the case this document also covers so that the warning is exercised.

openapi: 3.0.3

info:
  title: negotiation
  version: 1.0.0

paths:
  # One schema, three media types. The generated function takes a `contentType` and defaults it to
  # the one the generator prefers.
  /v1/documents/{id}:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    patch:
      operationId: patchDocument
      requestBody:
        required: true
        content:
          application/json-patch+json:
            schema: { ${'$'}ref: "#/components/schemas/Patch" }
          application/merge-patch+json:
            schema: { ${'$'}ref: "#/components/schemas/Patch" }
          application/strategic-merge-patch+json:
            schema: { ${'$'}ref: "#/components/schemas/Patch" }
      responses:
        "200":
          description: patched
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Document" }

  # One media type, so nothing changes: no `contentType` parameter and the literal header it always
  # had.
  /v1/documents:
    post:
      operationId: createDocument
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/Document" }
      responses:
        "201":
          description: created
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Document" }

components:
  schemas:
    Document:
      type: object
      required: [title]
      properties:
        title: { type: string }

    Patch:
      type: object
      properties:
        op: { type: string }
        path: { type: string }
        value: { type: string }
"""

private val featureMatrix: String = """openapi: 3.1.0
info:
  title: Feature Matrix
  version: 3.0.0
  description: Uses every schema, parameter, body, response and security feature kide generates, once.
servers:
  - url: https://{region}.api.example.com/v3
    variables:
      region: { default: eu }
security:
  - bearer: []
  - {}
tags:
  - name: shapes
paths:
  /shapes/{id}/{label}/{matrix}:
    parameters:
      - { name: id, in: path, required: true, schema: { type: integer, format: int64 } }
      - { name: label, in: path, required: true, style: label, schema: { type: string } }
      - { name: matrix, in: path, required: true, style: matrix, schema: { type: string } }
    get:
      operationId: getShape
      tags: [shapes]
      deprecated: true
      x-kapi-name: fetchShape
      parameters:
        - { name: kinds, in: query, schema: { type: array, items: { ${'$'}ref: '#/components/schemas/Kind' } } }
        - { name: sizes, in: query, explode: false, schema: { type: array, uniqueItems: true, items: { type: integer, format: int32 } } }
        - { name: pipes, in: query, style: pipeDelimited, explode: false, schema: { type: array, items: { type: string } } }
        - { name: spaces, in: query, style: spaceDelimited, explode: false, schema: { type: array, items: { type: number } } }
        - { name: filter, in: query, style: deepObject, schema: { type: object, additionalProperties: { type: string } } }
        - { name: raw, in: query, content: { application/json: { schema: { ${'$'}ref: '#/components/schemas/Point' } } } }
        - { name: since, in: query, schema: { type: string, format: date-time } }
        - { name: day, in: query, schema: { type: string, format: date } }
        - { name: token, in: query, schema: { type: string, format: uuid } }
        - { name: ratio, in: query, schema: { type: number, format: float, multipleOf: 0.25 } }
        - { name: exact, in: query, schema: { type: boolean } }
        - { name: price, in: query, schema: { type: string, format: decimal } }
        - { name: X-Request-Id, in: header, required: true, schema: { type: string }, x-kapi-sensitive: true }
        - { name: X-Tags, in: header, schema: { type: array, items: { type: string } } }
        - { name: tracking, in: cookie, schema: { type: string } }
        - { name: in, in: query, schema: { type: string } }
        - { name: Accept, in: header, schema: { type: string } }
      responses:
        '200':
          description: The shape
          content:
            application/json: { schema: { ${'$'}ref: '#/components/schemas/Shape' } }
            application/xml: { schema: { ${'$'}ref: '#/components/schemas/Shape' } }
        '204': { description: Nothing to show }
        '4XX':
          description: A client failure
          content:
            application/problem+json: { schema: { ${'$'}ref: '#/components/schemas/Problem' } }
        default:
          description: Any other failure
          content:
            text/plain: { schema: { type: string } }
    put:
      operationId: replaceShape
      tags: [shapes]
      security: [ { apiKey: [], session: [] } ]
      requestBody:
        content:
          application/json: { schema: { ${'$'}ref: '#/components/schemas/Shape' } }
      responses:
        '200':
          description: The raw bytes
          content:
            application/octet-stream: { schema: { type: string, format: binary } }
        '5XX': { description: A server failure }
  /forms:
    post:
      operationId: submitForm
      x-kapi-group: Forms
      security: [ { basic: [] } ]
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              required: [title, file]
              properties:
                title: { type: string }
                count: { type: integer }
                file: { type: string, format: binary }
                note: { type: string, format: binary }
      responses:
        '201': { description: Accepted }
  /forms/plain:
    post:
      operationId: submitPlain
      x-kapi-group: Forms
      security: [ { oauth: [write] }, { oidc: [] } ]
      requestBody:
        content:
          application/x-www-form-urlencoded:
            schema:
              type: object
              required: [name]
              properties:
                name: { type: string }
                age: { type: integer, format: int32 }
                when: { type: string, format: date-time }
      responses:
        '200':
          description: Echo
          content:
            text/plain: { schema: { type: string } }
  /ignored:
    get:
      x-kapi-ignore: true
      responses:
        '200': { description: never generated }
  /untagged/{id}:
    get:
      parameters:
        - { name: id, in: path, required: true, schema: { type: string } }
      requestBody:
        content:
          text/plain: { schema: { type: string } }
      responses:
        '200':
          description: Anything
          content:
            application/json: { schema: {} }
webhooks:
  shapeChanged:
    post:
      requestBody:
        content:
          application/json: { schema: { ${'$'}ref: '#/components/schemas/Shape' } }
      responses:
        '200': { description: ok }
components:
  securitySchemes:
    bearer: { type: http, scheme: bearer, bearerFormat: JWT }
    basic: { type: http, scheme: basic }
    digest: { type: http, scheme: digest }
    apiKey: { type: apiKey, name: key, in: query }
    session: { type: apiKey, name: SESSION, in: cookie }
    header: { type: apiKey, name: X-Api-Key, in: header }
    oauth:
      type: oauth2
      flows:
        clientCredentials: { tokenUrl: https://id.example.com/token, scopes: { write: writes } }
        password: { tokenUrl: /oauth/token, scopes: { write: writes, admin: administers } }
        authorizationCode:
          authorizationUrl: https://id.example.com/authorize
          tokenUrl: https://id.example.com/token
          scopes: { write: writes, admin: administers }
        implicit: { authorizationUrl: /oauth/authorize, scopes: { write: writes } }
    oidc: { type: openIdConnect, openIdConnectUrl: https://id.example.com/.well-known/openid-configuration }
    mtls: { type: mutualTLS }
  schemas:
    Kind: { type: string, enum: [circle, square, "3d", ""], x-kapi-enum-names: [ROUND, BOXY, THREE_D, BLANK] }
    Level: { type: integer, format: int32, enum: [1, 2, 3] }
    Big: { type: integer, enum: [10000000000, 20000000000], x-enum-varnames: [TEN, TWENTY] }
    Point:
      type: object
      required: [x, y]
      properties:
        x: { type: number, format: double, minimum: -10, maximum: 10 }
        y: { type: number, format: float, exclusiveMinimum: 0, exclusiveMaximum: 1.5 }
    Shape:
      type: object
      discriminator:
        propertyName: shapeType
        mapping: { circle: '#/components/schemas/Circle', square: '#/components/schemas/Square' }
      required: [shapeType, name]
      properties:
        shapeType: { type: string }
        name: { type: string, minLength: 1, maxLength: 40, pattern: '^[A-Za-z ]+${'$'}' }
        tags: { type: array, items: { type: string }, uniqueItems: true, minItems: 1, maxItems: 5 }
        center: { ${'$'}ref: '#/components/schemas/Point' }
        corners: { type: array, items: { ${'$'}ref: '#/components/schemas/Point' } }
    Payment:
      type: object
      description: Conditional, dependent and negated members
      required: [method]
      properties:
        method: { type: string, enum: [card, invoice] }
        card: { type: string }
        expiry: { type: string }
        iban: { type: string }
        currency: { type: string, const: EUR }
        legacy: false
        note: { type: string, not: { enum: [spam, junk] } }
      dependentRequired: { card: [expiry] }
      if: { properties: { method: { const: card } }, required: [method] }
      then: { required: [card] }
      else: { required: [iban] }
      not: { required: [card, iban] }
    Labels:
      type: object
      description: Members named by patterns
      properties:
        name: { type: string }
      propertyNames: { maxLength: 8 }
      patternProperties: { '^x-': { type: string, minLength: 3 } }
      additionalProperties: false
    Steps:
      type: object
      description: multipleOf on every numeric type
      required: [price, ratio, third, count]
      properties:
        price: { type: number, format: double, multipleOf: 0.01 }
        ratio: { type: number, format: float, multipleOf: 0.1 }
        third: { type: integer, format: int32, multipleOf: 1.5 }
        count: { type: integer, format: int64, multipleOf: 3.0 }
    Loose:
      type: object
      description: Conditions kide reports instead of checking
      properties:
        size: { type: integer }
      if: { properties: { size: { minimum: 3 } } }
      then: { required: [size] }
      not: { properties: { size: { const: 0 } } }
    Circle:
      allOf:
        - ${'$'}ref: '#/components/schemas/Shape'
        - type: object
          required: [radius]
          properties:
            radius: { type: integer, format: int64, minimum: 1, multipleOf: 2 }
            level: { ${'$'}ref: '#/components/schemas/Level' }
    Square:
      allOf:
        - ${'$'}ref: '#/components/schemas/Shape'
        - type: object
          properties:
            side: { type: integer, format: int32, exclusiveMinimum: 0, maximum: 100 }
            big: { ${'$'}ref: '#/components/schemas/Big' }
    Value:
      description: Any of several JSON shapes, decided by content
      oneOf:
        - { type: string }
        - { type: integer }
        - { type: boolean }
        - { type: array, items: { type: string } }
        - { type: object, additionalProperties: { type: integer } }
        - { ${'$'}ref: '#/components/schemas/Point' }
        - { ${'$'}ref: '#/components/schemas/Kind' }
    MaybeValue:
      anyOf:
        - { ${'$'}ref: '#/components/schemas/Point' }
        - { type: 'null' }
    Defaults:
      type: object
      properties:
        text: { type: string, default: hello }
        whole: { type: integer, format: int32, default: 7 }
        long: { type: integer, default: 9 }
        real: { type: number, default: 1.5 }
        small: { type: number, format: float, default: 0.5 }
        flag: { type: boolean, default: true }
        kind: { ${'$'}ref: '#/components/schemas/Kind', default: square }
        list: { type: array, items: { type: string }, default: [] }
        set: { type: array, uniqueItems: true, items: { type: string }, default: [] }
        map: { type: object, additionalProperties: { type: string }, default: {} }
        object: { ${'$'}ref: '#/components/schemas/Point', default: { x: 1, y: 0.5 } }
    Everything:
      type: object
      title: Everything at once
      deprecated: true
      x-kapi-name: AllOfIt
      required: [id, secret, created, readOnlyId, writeOnlyKey]
      properties:
        id: { type: string, format: uuid }
        secret: { type: string, x-kapi-sensitive: true }
        created: { type: string, format: date-time, readOnly: false }
        readOnlyId: { type: string, readOnly: true }
        writeOnlyKey: { type: string, writeOnly: true }
        birthday: { type: string, format: date }
        amount: { type: string, format: decimal }
        blob: { type: string, format: byte }
        exact: { type: number, format: decimal }
        money: { type: string, x-kapi-type: kotlin.String }
        untyped: {}
        nothing: { type: 'null' }
        constant: { const: fixed }
        tuple: { type: array, prefixItems: [ { type: string } ], items: false }
        bare: { items: { type: string } }
        bareMap: { additionalProperties: { type: boolean } }
        anyObject: { type: object }
        nullableText: { type: [string, 'null'] }
        mixed: { type: [string, integer] }
        labels: { type: object, additionalProperties: { type: string }, minProperties: 1, maxProperties: 3 }
        nestedList: { type: array, items: { type: array, items: { ${'$'}ref: '#/components/schemas/Point' } } }
        ignored: { type: string, x-kapi-ignore: true }
        renamed: { type: string, x-kapi-name: better }
        value: { ${'$'}ref: '#/components/schemas/Value' }
        maybe: { ${'$'}ref: '#/components/schemas/MaybeValue' }
        inline:
          type: object
          properties:
            deep: { type: string, enum: [a, b] }
        inlineUnion:
          oneOf:
            - { type: string }
            - { type: object, properties: { a: { type: string } } }
        alias: { ${'$'}ref: '#/components/schemas/Alias' }
        aliasOfAlias: { ${'$'}ref: '#/components/schemas/AliasOfAlias' }
    Open:
      type: object
      additionalProperties: true
      properties:
        known: { type: string }
    Typed:
      type: object
      additionalProperties: { ${'$'}ref: '#/components/schemas/Point' }
      properties:
        known: { type: integer }
    Empty: { type: object }
    Alias: { type: array, items: { type: string, format: date-time } }
    AliasOfAlias: { ${'$'}ref: '#/components/schemas/Alias' }
    Problem:
      type: object
      required: [title]
      properties:
        title: { type: string }
        status: { type: integer, format: int32 }
"""

private val hostile: String = """# A document written to break the generator.
#
# Every text the document controls carries a payload that leaves a Kotlin string, raw string, string
# template, block comment or KotlinPoet format string if it is not escaped, and then calls
# `kideInjected()` - a function that does not exist. Generated code that compiles therefore has no
# injected code. Names collide by case, by sanitising and with kide's own support files, so every file
# path must stay unique. Numbers use every YAML form.

openapi: 3.1.0
info:
  title: Hostile
  version: "1.0 \" + kideInjected() + \" \u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662\u2662 kideInjected() \r kideInjected() \u2028 kideInjected() \u00b7"
  description: "q\" + kideInjected() + \"q \"\"\" + kideInjected() + \"\"\" ${'$'}{kideInjected()} ${'$'}kideInjected */ kideInjected() /* % %S %L %% \u00b7 \u2662 \u2028 \r\n\\ end"
servers:
  - url: "https://{region}.example.com/q\"\"\"+kideInjected()+\"\"\"/${'$'}kideInjected"
    description: "q\" + kideInjected() + \"q \"\"\" + kideInjected() + \"\"\" ${'$'}{kideInjected()} */ kideInjected() /* %S \u00b7 \u2662"
    variables:
      region:
        default: "e\"u${'$'}x"
        enum: ["e\"u${'$'}x", "u%s\u00b7\u2662"]
        description: "q\" + kideInjected() + \"q \"\"\" + kideInjected() + \"\"\" */ kideInjected() /*"
security:
  - oauth: ["read\" + kideInjected() + \"", "write ${'$'}kideInjected", "a\") + listOf(\"INJ"]
tags:
  - name: pet
  - name: Pet
  - name: pet-store
  - name: pet_store
paths:
  "/hostile/q\"\"\"+kideInjected()+\"\"\"/${'$'}kideInjected/*/{id}":
    get:
      operationId: "get\" + kideInjected() + \""
      summary: "q\" + kideInjected() + \"q \"\"\" + kideInjected() + \"\"\" ${'$'}{kideInjected()} */ kideInjected() /* %S \u00b7 \u2662"
      description: "q\" + kideInjected() + \"q \"\"\" + kideInjected() + \"\"\" ${'$'}{kideInjected()} */ kideInjected() /* %S \u00b7 \u2662 \r\n end"
      tags: [pet]
      parameters:
        - name: id
          in: path
          required: true
          description: "q\" + kideInjected() + \"q */ kideInjected() /* %S"
          schema: { type: string, minLength: 1, maxLength: 3, pattern: "^[a-z\"${'$'}%\\\\]+\u00b7?\u2662?${'$'}" }
        - name: "q\" + kideInjected() + \"q"
          in: query
          schema: { type: string, default: "q\" + kideInjected() + \"q ${'$'}{kideInjected()} \u00b7 \u2662 %S \r\n end" }
        - name: "X-Header\"${'$'}x"
          in: header
          schema: { type: integer, format: int64, minimum: 1.5e3, maximum: 2147483647, default: 1e3 }
        - name: object
          in: query
          x-kapi-name: "`a`: String? = kideInjected(), `b`"
          schema: { type: number, minimum: -1.0E10, maximum: 15, exclusiveMaximum: 1e300 }
        - name: "c${'$'}k"
          in: cookie
          schema: { type: string, enum: ["50%", "%%", "a\u00b7b", "a\u2662b", "q\"${'$'}x", "line\nbreak", "cr\rlf"] }
      responses:
        "200":
          description: "q\" + kideInjected() + \"q */ kideInjected() /* %S \u00b7 \u2662"
          headers:
            "X-Rate\"${'$'}x":
              description: "q\" + kideInjected() + \"q */ kideInjected() /*"
              schema: { type: integer }
          content:
            "application/json":
              schema: { ${'$'}ref: '#/components/schemas/Hostile' }
          links:
            "next\" + kideInjected() + \"":
              operationId: "get\" + kideInjected() + \""
              parameters:
                id: "${'$'}response.body#/q\"\"\"+kideInjected()+\"\"\""
        default:
          description: "q\" + kideInjected() + \"q */ kideInjected() /*"
          content:
            "text/plain": { schema: { type: string } }
    post:
      operationId: "post\" + kideInjected() + \""
      tags: [Pet]
      x-kapi-name: "`post`() = kideInjected()\nfun `x`"
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: string }
      requestBody:
        required: true
        content:
          "application/json":
            schema: { ${'$'}ref: '#/components/schemas/Choice' }
      responses:
        "201": { description: "Created \"\"\" + kideInjected() + \"\"\"" }
      callbacks:
        "on\" + kideInjected() + \"":
          "{${'$'}request.body#/url}/q\"\"\"+kideInjected()+\"\"\"":
            post:
              requestBody:
                content:
                  "application/json":
                    schema: { ${'$'}ref: '#/components/schemas/Hostile' }
              responses:
                "200": { description: "q\" + kideInjected() + \"q */ kideInjected() /*" }
  /pet-store/things:
    get:
      operationId: storeThings
      tags: [pet-store]
      responses:
        "200": { description: ok }
  /pet_store/things:
    get:
      operationId: storeThingsAgain
      tags: [pet_store]
      responses:
        "200": { description: ok }
webhooks:
  "hook\" + kideInjected() + \"":
    post:
      requestBody:
        content:
          "application/json":
            schema: { ${'$'}ref: '#/components/schemas/PetOwner' }
      responses:
        "200": { description: ok }
components:
  securitySchemes:
    oauth:
      type: oauth2
      description: "q\" + kideInjected() + \"q */ kideInjected() /*"
      flows:
        clientCredentials:
          tokenUrl: https://id.example.com/token
          scopes:
            "read\" + kideInjected() + \"": "q\" + kideInjected() + \"q */ kideInjected() /*"
            "write ${'$'}kideInjected": "write"
            "a\") + listOf(\"INJ": "x"
    "key\" + kideInjected() + \"":
      type: apiKey
      in: header
      name: "X-Key\" + kideInjected() + \"${'$'}x"
  schemas:
    Hostile:
      type: object
      description: "q\" + kideInjected() + \"q \"\"\" + kideInjected() + \"\"\" ${'$'}{kideInjected()} */ kideInjected() /* %S \u00b7 \u2662"
      title: "q\" + kideInjected() + \"q */ kideInjected() /*"
      required: [object, in]
      additionalProperties: true
      properties:
        object: { type: string, minLength: 2, maxLength: 4 }
        in: { type: string, x-kapi-sensitive: true }
        is: { type: integer, format: int32, default: 2147483647, minimum: -2147483648 }
        fun: { type: integer, format: int64, default: -9223372036854775808, maximum: 9223372036854775807 }
        class: { type: number, default: 1.5e3, minimum: 1e-3 }
        this: { type: number, format: float, default: 3.4e38 }
        "null": { type: boolean, default: true }
        if: { type: string, default: "crlf\r\nline\u2028end" }
        "x\", \"INJ": { type: string, description: "q\" + kideInjected() + \"q */ kideInjected() /*" }
        "${'$'}kideInjected": { type: string }
        "${'$'}{kideInjected()}": { type: string, x-kapi-sensitive: true }
        "back\\slash": { type: string }
        named:
          type: string
          x-kapi-name: "`a`: String? = kideInjected(), public val `b`"
        legal: { type: string, x-kapi-name: object }
        mood: { ${'$'}ref: '#/components/schemas/Mood' }
        level: { ${'$'}ref: '#/components/schemas/Level' }
        flag: { ${'$'}ref: '#/components/schemas/Flag' }
        ratio: { ${'$'}ref: '#/components/schemas/Ratio' }
        emoji: { type: string, minLength: 1, maxLength: 1, pattern: "^\\p{So}${'$'}" }
        example: { type: string, example: "q\" + kideInjected() + \"q */ kideInjected() /*" }
    Mood:
      type: string
      description: "q\" + kideInjected() + \"q */ kideInjected() /*"
      enum: ["50%", "%%", "%S", "a\u00b7b", "a\u2662b", "q\"${'$'}x", "q\"\"\"x", "line\nbreak", "cr\rlf", "\\", "object", "in"]
      x-kapi-enum-names: ["`A`(\"\"), `B`", "fun", "`C`", object, "1x", "a\u2662b", "Q", "R", "S", "T", "U", "V"]
    Level:
      type: integer
      format: int32
      enum: [1, 2, 16, 7]
    Flag:
      type: boolean
      enum: [true, false]
    Ratio:
      type: number
      enum: [0.5, 1.5e3]
    Choice:
      oneOf:
        - ${'$'}ref: '#/components/schemas/PetOwner'
        - ${'$'}ref: '#/components/schemas/Petowner'
      discriminator:
        propertyName: "kind\" + kideInjected() + \""
        mapping:
          "owner\" + kideInjected() + \"": '#/components/schemas/PetOwner'
          "owner \u00b7 \u2662 %S ${'$'}x": '#/components/schemas/Petowner'
    PetOwner:
      type: object
      required: ["kind\" + kideInjected() + \"", "name\"${'$'}x"]
      properties:
        "kind\" + kideInjected() + \"": { type: string }
        "name\"${'$'}x": { type: string }
    Petowner:
      type: object
      required: ["kind\" + kideInjected() + \""]
      properties:
        "kind\" + kideInjected() + \"": { type: string }
        "other\u00b7\u2662": { type: string }
    pet_owner: { type: string }
    kidePatch: { type: object, properties: { a: { type: string } } }
    kideFilePart: { type: object, properties: { a: { type: string } } }
    kideCbor: { type: object, properties: { a: { type: string } } }
    HostileJson: { type: object, properties: { a: { type: string } } }
    HostileCodecs: { type: object, properties: { a: { type: string } } }
    "Hostile\" + kideInjected() + \"": { type: string }
"""

private val kitchenSink: String = """# The document the generator is held to.
#
# Not a real service: every path here exists because it is a shape the generator has to get right,
# and the module around it exists so that "the generated code compiles" is something `./gradlew
# build` decides rather than something somebody checks by hand. Its output is committed, so a change
# in what the generator emits is a diff in a merge request.

openapi: 3.0.3

info:
  title: Generator Fixture
  version: 1.0.0
  description: >-
    Every shape the generator has to handle, in one document.

security:
  - bearerAuth: []

tags:
  - name: widgets
    description: A collection with an item and an action under it.

paths:
  /v1/widgets:
    get:
      operationId: listWidgets
      tags: [widgets]
      summary: A collection with query parameters of every optionality.
      parameters:
        - { name: state, in: query, schema: { ${'$'}ref: "#/components/schemas/WidgetState" } }
        - { name: pageSize, in: query, schema: { type: integer, format: int32, default: 50 } }
        - { name: pageToken, in: query, schema: { type: string } }
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: The page.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/WidgetPage" }
    post:
      operationId: createWidget
      tags: [widgets]
      summary: A body, and a header parameter alongside it.
      parameters:
        - name: Idempotency-Key
          in: header
          schema: { type: string }
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/NewWidget" }
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "201":
          description: Made.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Widget" }

  /v1/widgets/{id}:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    get:
      operationId: getWidget
      tags: [widgets]
      summary: An item under a placeholder.
      parameters:
        - name: If-None-Match
          in: header
          schema: { type: string }
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: It, with the revision it is at.
          headers:
            ETag:
              description: The revision, so the next call can be a `304`.
              schema: { type: string }
            X-Widget-Kind:
              description: Declared but not always sent, which is the optional case.
              schema: { type: string }
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Widget" }
        "304": { description: The caller already holds this revision. }
        "404": { description: There is no such widget. }

  # The shape that broke the resource tree once: an action two levels down whose intermediate
  # placeholder is never a path of its own.
  /v1/widgets/{id}/retire:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    post:
      operationId: retireWidget
      tags: [widgets]
      summary: An action under an item, answering only statuses without bodies.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "204": { description: Retired. }
        "409": { description: It was already retired. }

  # A sibling of the placeholder rather than a child of it, which is what makes the tree a tree.
  /v1/widgets/kinds:
    get:
      operationId: listKinds
      tags: [widgets]
      summary: An operation taking nothing at all.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: Them.
          content:
            application/json:
              schema:
                type: array
                items: { type: string }

  # A name with slashes in it: one placeholder in the document, several segments on the wire.
  /v1/vault/{path}:
    parameters:
      - name: path
        in: path
        required: true
        x-tail: true
        description: The secret as the caller thinks of it, `services/orders/database`.
        schema: { type: string }
    get:
      operationId: readVaultEntry
      tags: [widgets]
      summary: A path parameter that stands for the rest of the path.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: It.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Health" }

  # A multipart body: several named parts, one of them binary, one of them optional.
  /v1/widgets/import:
    post:
      operationId: importWidgets
      tags: [widgets]
      summary: A multipart/form-data body.
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              required: [file, name]
              properties:
                file:
                  type: string
                  format: binary
                name:
                  type: string
                note:
                  type: string
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "202":
          description: Taken.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Health" }

  # A whole body of bytes, in and out: streamed rather than held.
  /v1/blobs/{id}:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    put:
      operationId: uploadBlob
      tags: [widgets]
      summary: A binary request body.
      requestBody:
        required: true
        content:
          application/octet-stream:
            schema: { type: string, format: binary }
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "204": { description: Stored. }
    get:
      operationId: downloadBlob
      tags: [widgets]
      summary: A binary response body.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: The object.
          content:
            application/octet-stream:
              schema: { type: string, format: binary }
        "404": { description: There is no such object. }

  /v1/health:
    get:
      operationId: health
      summary: The one route a certificate alone reaches.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: Up.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Health" }

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer

  responses:
    ClientProblem:
      description: The request was refused.
      content:
        application/problem+json:
          schema: { ${'$'}ref: "#/components/schemas/Problem" }
    ServerProblem:
      description: The service could not answer.
      content:
        application/problem+json:
          schema: { ${'$'}ref: "#/components/schemas/Problem" }

  schemas:
    Problem:
      type: object
      required: [type, title, status]
      properties:
        type: { type: string }
        title: { type: string }
        status: { type: integer, format: int32 }
        detail: { type: string }

    Health:
      type: object
      required: [status]
      properties:
        status: { type: string }

    WidgetState:
      type: string
      enum: [draft, live, retired]

    NewWidget:
      type: object
      required: [name]
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 64
        labels:
          type: object
          additionalProperties: { type: string }
        tags:
          type: array
          minItems: 1
          items: { type: string }

    Widget:
      type: object
      required: [id, name, state, revision]
      properties:
        id: { type: string }
        name: { type: string }
        state: { ${'$'}ref: "#/components/schemas/WidgetState" }
        revision:
          type: integer
          format: int64
        # Deprecated on a property, which the generator used to ignore entirely. The annotation goes
        # on the property and never on the constructor parameter: a deprecated parameter makes every
        # construction of the model a warning, and every module here builds warnings as errors.
        legacyName:
          type: string
          deprecated: true
          description: Use `name` instead.
        labels:
          type: object
          additionalProperties: { type: string }

    WidgetPage:
      type: object
      required: [widgets]
      properties:
        widgets:
          type: array
          items: { ${'$'}ref: "#/components/schemas/Widget" }
        nextPageToken: { type: string }
"""

private const val mailRoutes: String = """# The path-tree shapes, which between them cover every case the resource generator has to
# get right: a collection with query parameters, an item under a placeholder, an action
# under that item, and a sub-collection that is a sibling of the placeholder rather than a
# child of it.

openapi: 3.0.3
info:
  title: mail
  version: "1"
paths:
  /v1/mail:
    get:
      operationId: listMail
      parameters:
        - { name: state, in: query, schema: { type: string } }
        - { name: pageSize, in: query, required: true, schema: { type: integer } }
      responses:
        "200": { description: ok }
    post:
      operationId: sendMail
      responses:
        "202": { description: ok }
  /v1/mail/{id}:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    get:
      operationId: getMail
      responses:
        "200": { description: ok }
  /v1/mail/{id}/cancel:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    post:
      operationId: cancelMail
      responses:
        "204": { description: ok }
  /v1/mail/templates/{name}:
    parameters:
      - { name: name, in: path, required: true, schema: { type: string } }
    get:
      operationId: getTemplate
      responses:
        "200": { description: ok }
"""

private val mail: String = """# Sealed answers, use cases and skeletons.
#
# `cancelMail` is the case that used to be invisible: the document declares `204` and `409`, and the
# generated controller only ever knew about the success type. A service therefore wrote
# `call.respond(HttpStatusCode.Conflict)` by hand, and nothing checked the document had said so.
#
# Three near-identical copies of this document used to live inside three test files and had drifted
# apart. This is the one of them.

openapi: 3.0.3

info:
  title: mail
  version: "1"

paths:
  /v1/mail:
    get:
      operationId: listMail
      responses:
        "200":
          description: the page
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/MailPage" }

  # An action two levels down, so that `{id}` is never a leaf and the route has to read it through
  # `parent`.
  /v1/mail/{id}/cancel:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    post:
      operationId: cancelMail
      responses:
        "204": { description: cancelled }
        "409": { description: already gone }
        "4XX":
          description: refused
          content:
            application/problem+json:
              schema: { ${'$'}ref: "#/components/schemas/Problem" }

  # An operation that takes nothing, so the use case is `UseCase<Unit, R>` and gets no input type.
  /v1/mail/health:
    get:
      operationId: health
      responses:
        "200": { description: ok }

components:
  schemas:
    Problem:
      type: object
      required: [title]
      properties:
        title: { type: string }

    MailPage:
      type: object
      required: [mail]
      properties:
        mail:
          type: array
          items: { type: string }
"""

private val neutral: String = """# The document the generator is held to.
#
# Not a real service: every path here exists because it is a shape the generator has to get right,
# and the module around it exists so that "the generated code compiles" is something `./gradlew
# build` decides rather than something somebody checks by hand. Its output is committed, so a change
# in what the generator emits is a diff in a merge request.

openapi: 3.0.3

info:
  title: Generator Fixture
  version: 1.0.0
  description: >-
    Every shape the generator has to handle, in one document.

security:
  - bearerAuth: []

tags:
  - name: widgets
    description: A collection with an item and an action under it.

paths:
  /v1/widgets:
    get:
      operationId: listWidgets
      tags: [widgets]
      summary: A collection with query parameters of every optionality.
      parameters:
        - { name: state, in: query, schema: { ${'$'}ref: "#/components/schemas/WidgetState" } }
        - { name: pageSize, in: query, schema: { type: integer, format: int32, default: 50 } }
        - { name: pageToken, in: query, schema: { type: string } }
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: The page.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/WidgetPage" }
    post:
      operationId: createWidget
      tags: [widgets]
      summary: A body, and a header parameter alongside it.
      parameters:
        - name: Idempotency-Key
          in: header
          schema: { type: string }
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/NewWidget" }
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "201":
          description: Made.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Widget" }
          # What the document already knew and nothing read: the answer carries the id, and
          # `getWidget` is the operation that takes one.
          links:
            widget:
              operationId: getWidget
              parameters:
                id: "${'$'}response.body#/id"

  /v1/widgets/{id}:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    get:
      operationId: getWidget
      tags: [widgets]
      summary: An item under a placeholder.
      parameters:
        - name: If-None-Match
          in: header
          schema: { type: string }
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: It, with the revision it is at.
          headers:
            ETag:
              description: The revision, so the next call can be a `304`.
              schema: { type: string }
            X-Widget-Kind:
              description: Declared but not always sent, which is the optional case.
              schema: { type: string }
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Widget" }
        "304": { description: The caller already holds this revision. }
        "404": { description: There is no such widget. }

  # The shape that broke the resource tree once: an action two levels down whose intermediate
  # placeholder is never a path of its own.
  /v1/widgets/{id}/retire:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    post:
      operationId: retireWidget
      tags: [widgets]
      summary: An action under an item, answering only statuses without bodies.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "204": { description: Retired. }
        "409": { description: It was already retired. }

  # A sibling of the placeholder rather than a child of it, which is what makes the tree a tree.
  /v1/widgets/kinds:
    get:
      operationId: listKinds
      tags: [widgets]
      summary: An operation taking nothing at all.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: Them.
          content:
            application/json:
              schema:
                type: array
                items: { type: string }

  # A name with slashes in it: one placeholder in the document, several segments on the wire.
  /v1/vault/{path}:
    parameters:
      - name: path
        in: path
        required: true
        x-tail: true
        description: The secret as the caller thinks of it, `services/orders/database`.
        schema: { type: string }
    get:
      operationId: readVaultEntry
      tags: [widgets]
      summary: A path parameter that stands for the rest of the path.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: It.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Health" }

  # A multipart body: several named parts, one of them binary, one of them optional.
  /v1/widgets/import:
    post:
      operationId: importWidgets
      tags: [widgets]
      summary: A multipart/form-data body.
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              required: [file, name]
              properties:
                file:
                  type: string
                  format: binary
                name:
                  type: string
                note:
                  type: string
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "202":
          description: Taken.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Health" }

  # A whole body of bytes, in and out: streamed rather than held.
  /v1/blobs/{id}:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    put:
      operationId: uploadBlob
      tags: [widgets]
      summary: A binary request body.
      requestBody:
        required: true
        content:
          application/octet-stream:
            schema: { type: string, format: binary }
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "204": { description: Stored. }
    get:
      operationId: downloadBlob
      tags: [widgets]
      summary: A binary response body.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: The object.
          content:
            application/octet-stream:
              schema: { type: string, format: binary }
        "404": { description: There is no such object. }

  /v1/health:
    get:
      operationId: health
      summary: The one route a certificate alone reaches.
      responses:
        "4XX": { ${'$'}ref: "#/components/responses/ClientProblem" }
        "5XX": { ${'$'}ref: "#/components/responses/ServerProblem" }
        "200":
          description: Up.
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Health" }

  # A handshake rather than a request. `send` and `receive` name the message schemas, and the
  # generator emits a session on both sides; without a document declaring one, everything the
  # WebSocket generator writes was emitted nowhere at all.
  /v1/widgets/watch:
    get:
      operationId: watchWidgets
      tags: [widgets]
      summary: A WebSocket session over the same models the HTTP routes use.
      x-websocket:
        send: { ${'$'}ref: "#/components/schemas/WatchCommand" }
        receive: { ${'$'}ref: "#/components/schemas/WidgetEvent" }
      responses:
        "101": { description: Switching protocols. }

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer

  responses:
    ClientProblem:
      description: The request was refused.
      content:
        application/problem+json:
          schema: { ${'$'}ref: "#/components/schemas/Problem" }
    ServerProblem:
      description: The service could not answer.
      content:
        application/problem+json:
          schema: { ${'$'}ref: "#/components/schemas/Problem" }

  schemas:
    Problem:
      type: object
      required: [type, title, status]
      properties:
        type: { type: string }
        title: { type: string }
        status: { type: integer, format: int32 }
        detail: { type: string }

    Health:
      type: object
      required: [status]
      properties:
        status: { type: string }

    WidgetState:
      type: string
      enum: [draft, live, retired]

    NewWidget:
      type: object
      required: [name]
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 64
        labels:
          type: object
          additionalProperties: { type: string }
        tags:
          type: array
          minItems: 1
          items: { type: string }

    Widget:
      type: object
      required: [id, name, state, revision]
      properties:
        id: { type: string }
        name: { type: string }
        state: { ${'$'}ref: "#/components/schemas/WidgetState" }
        revision:
          type: integer
          format: int64
        # Deprecated on a property, which the generator used to ignore entirely. The annotation goes
        # on the property and never on the constructor parameter: a deprecated parameter makes every
        # construction of the model a warning, and every module here builds warnings as errors.
        legacyName:
          type: string
          deprecated: true
          description: Use `name` instead.
        labels:
          type: object
          additionalProperties: { type: string }

    WidgetPage:
      type: object
      required: [widgets]
      properties:
        widgets:
          type: array
          items: { ${'$'}ref: "#/components/schemas/Widget" }
        nextPageToken: { type: string }

    # An enum a deployment is allowed to extend. Honoured only under `model { extensibleEnums = true }`,
    # which the `neutral` configuration turns on - so this and the plain enum above are the two enum
    # shapes side by side.
    WidgetTier:
      type: string
      x-extensible-enum: [bronze, silver, gold]

    # A `oneOf` told apart by the shape of its members rather than by a discriminator property. The
    # deduction is a different branch of the model generator from the discriminated case.
    WatchCommand:
      x-jackson-subtype-deduction: true
      oneOf:
        - ${'$'}ref: "#/components/schemas/WatchAll"
        - ${'$'}ref: "#/components/schemas/WatchOne"

    WatchAll:
      type: object
      required: [since]
      properties:
        since: { type: string, format: date-time }

    WatchOne:
      type: object
      required: [id]
      properties:
        id: { type: string }

    WidgetEvent:
      type: object
      required: [widget, tier]
      properties:
        widget: { ${'$'}ref: "#/components/schemas/Widget" }
        tier: { ${'$'}ref: "#/components/schemas/WidgetTier" }
"""

private val openobject: String = """# A schema that names some of its properties and admits others.
#
# `properties` beside `additionalProperties` used to end generation with an
# `UnsupportedOperationException` saying kotlinx cannot represent it. There is no *automatic*
# representation - a bag of unknown values would have to be `Any?`, which nothing serialises - but a
# serialiser written by hand can do it, and its shape is entirely mechanical.

openapi: 3.0.3

info:
  title: open-objects
  version: 1.0.0

paths:
  /v1/records:
    post:
      operationId: putRecord
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/Record" }
      responses:
        "200":
          description: stored
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Record" }

components:
  schemas:
    # Named and unnamed at once.
    Record:
      type: object
      required: [id]
      additionalProperties: true
      properties:
        id: { type: string }
        label: { type: string }

    # Only unnamed, which is an ordinary map and needs none of this.
    Labels:
      type: object
      additionalProperties: { type: string }
"""

private val outbound: String = """# Operations that travel the other way.
#
# Everything else in this generator describes a request a caller sends and an answer it gets back.
# These two describe the reverse - the API calling the caller - and until now both produced nothing
# at all. A document declaring them generated as though it had not.
#
#   * A **callback** is declared on the operation that arms it, and its key is the runtime expression
#     saying where to send it. The generator cannot evaluate that expression, so the sender takes the
#     URL as a parameter and the receiver mounts wherever the application puts it.
#   * A **webhook** (3.1) is the same shape without the arming operation: the API sends it because
#     the consumer subscribed out of band.
#
# Neither names a path the generator could mount on, which is the whole reason they are not just more
# entries under `paths`.

openapi: 3.1.0

info:
  title: callbacks
  version: 1.0.0

paths:
  /v1/jobs:
    post:
      operationId: createJob
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/NewJob" }
      responses:
        "202":
          description: accepted
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Job" }
      callbacks:
        jobFinished:
          "{${'$'}request.body#/callbackUrl}":
            post:
              operationId: jobFinished
              requestBody:
                required: true
                content:
                  application/json:
                    schema: { ${'$'}ref: "#/components/schemas/JobResult" }
              responses:
                "204": { description: taken }
                "410":
                  description: gone, stop sending
                  content:
                    application/json:
                      schema: { ${'$'}ref: "#/components/schemas/Problem" }

webhooks:
  # A webhook with a body and nothing to say back.
  jobCancelled:
    post:
      operationId: jobCancelled
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/JobResult" }
      responses:
        "204": { description: taken }

components:
  schemas:
    NewJob:
      type: object
      required: [callbackUrl]
      properties:
        callbackUrl: { type: string }

    Job:
      type: object
      required: [id]
      properties:
        id: { type: string }

    JobResult:
      type: object
      required: [id, state]
      properties:
        id: { type: string }
        state: { type: string }

    Problem:
      type: object
      properties:
        title: { type: string }
"""

private val parametersConstraints: String = """# What a parameter's schema says it has to be.
#
# These were parsed, carried on the parameter and read by nothing: a document could bound `pageSize`
# at 100 and every value went through. Worse, the branch that rebuilds a parameter after a name
# clash copied only two of the four constraints it carried, so the other two vanished silently.
#
# The client checks before the round trip, so a caller sees the bound they broke at their own call
# site. The server checks after binding, because a client is not the only thing that will call it.

openapi: 3.0.3

info:
  title: constraints
  version: 1.0.0

paths:
  /v1/search:
    get:
      operationId: search
      parameters:
        # Optional with a bound: absent is fine, out of range is not.
        - name: pageSize
          in: query
          schema:
            type: integer
            format: int32
            minimum: 1
            maximum: 100

        # Required, so the check needs no null guard.
        - name: q
          in: query
          required: true
          schema:
            type: string
            minLength: 2
            maxLength: 64

        # A pattern, which has to survive being emitted as a Kotlin literal with its backslashes.
        - name: since
          in: query
          schema:
            type: string
            pattern: "^\\d{4}-\\d{2}-\\d{2}${'$'}"

        # A format the generator can check without a library.
        - name: notify
          in: query
          schema:
            type: string
            format: email

        # A header carries rules the same way a query parameter does.
        - name: X-Tenant
          in: header
          required: true
          schema:
            type: string
            minLength: 1

        # An exclusive bound reads differently from an inclusive one.
        - name: ratio
          in: query
          schema:
            type: number
            format: double
            minimum: 0
            exclusiveMinimum: true
            maximum: 1

      responses:
        "200":
          description: results
          content:
            application/json:
              schema:
                type: array
                items: { type: string }
        "400": { description: refused }
"""

private val parametersCookie: String = """# Cookie parameters.
#
# These used to end generation with an `IllegalStateException` thrown inside the constructor of a
# single parameter, which read like a defect in the generator rather than a shape it did not
# support. Both Ory Hydra and Kratos deal in session cookies, so an upstream document update was one
# `in: cookie` away from a build that could not be explained.
#
# A cookie is not a property of an `@Resource` class: Ktor decodes those from the path and the query
# string. So the server reads them off the call beside the headers.

openapi: 3.0.3

info:
  title: cookies
  version: 1.0.0

paths:
  /v1/session:
    get:
      operationId: readSession
      parameters:
        # Required: missing is a malformed request, the same as a missing required header.
        - name: ory_session
          in: cookie
          required: true
          schema: { type: string }
        # Optional: absent means absent, not empty.
        - name: csrf_token
          in: cookie
          schema: { type: string }
        # Alongside a header and a query parameter, so the order they reach the controller in is
        # part of what is recorded.
        - name: X-Request-Id
          in: header
          schema: { type: string }
        - name: verbose
          in: query
          schema: { type: boolean }
      responses:
        "200":
          description: the session
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Session" }
        "401": { description: no session }

components:
  schemas:
    Session:
      type: object
      required: [id]
      properties:
        id: { type: string }
        active: { type: boolean }
"""

private val parametersPathStyles: String = """# Path parameters written as `label` and `matrix`, and query parameters converted back.
#
# Two things this document is here to hold down.
#
#   * `label` and `matrix` put a prefix in front of the value - `.red`, `;colour=red` - and replace
#     the placeholder rather than filling it in. Ktor's route matcher cannot express either as a
#     segment pattern, so the property is the raw segment and the route takes it apart. Until this
#     existed, a document using either was refused outright.
#   * A parameter the resources format cannot bind arrives as text, and the controller used to be
#     handed that text however the schema was typed. Here the schemas are deliberately not strings,
#     so a generated signature saying `Int` is a signature the route has to make true.

openapi: 3.0.3

info:
  title: path-styles
  version: 1.0.0

paths:
  # `label`, one value: `/v1/points/.42`.
  /v1/points/{index}:
    get:
      operationId: getPoint
      parameters:
        - name: index
          in: path
          required: true
          style: label
          schema: { type: integer }
      responses:
        "200":
          description: the point
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Point" }

  # `label`, exploded list: `/v1/traces/.a.b.c`.
  /v1/traces/{segments}:
    get:
      operationId: getTrace
      parameters:
        - name: segments
          in: path
          required: true
          style: label
          explode: true
          schema:
            type: array
            items: { type: string }
      responses:
        "200":
          description: the trace
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Point" }

  # `matrix`, one value: `/v1/tiles/;id=7`.
  /v1/tiles/{id}:
    get:
      operationId: getTile
      parameters:
        - name: id
          in: path
          required: true
          style: matrix
          schema: { type: integer, format: int64 }
      responses:
        "200":
          description: the tile
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Point" }

  # `matrix`, exploded object: `/v1/regions/;x=1;y=2`, where the parameter's own name never appears.
  /v1/regions/{corner}:
    get:
      operationId: getRegion
      parameters:
        - name: corner
          in: path
          required: true
          style: matrix
          explode: true
          schema: { ${'$'}ref: "#/components/schemas/Point" }
      responses:
        "200":
          description: the region
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Point" }

  # A query parameter the resources format cannot bind, whose schema is not a string: the route
  # reads `x=1&y=2` and hands the controller a `Point`, and `sizes=1,2,3` becomes a `List<Int>`.
  /v1/areas:
    get:
      operationId: listAreas
      parameters:
        - name: origin
          in: query
          required: true
          style: deepObject
          explode: true
          schema: { ${'$'}ref: "#/components/schemas/Point" }
        - name: sizes
          in: query
          required: false
          style: form
          explode: false
          schema:
            type: array
            items: { type: integer }
        - name: shape
          in: query
          required: false
          style: pipeDelimited
          explode: false
          schema:
            type: array
            items: { ${'$'}ref: "#/components/schemas/Corner" }
      responses:
        "200":
          description: the areas
          content:
            application/json:
              schema:
                type: array
                items: { ${'$'}ref: "#/components/schemas/Point" }

components:
  schemas:
    Point:
      type: object
      required: [x, y]
      properties:
        x: { type: integer }
        y: { type: integer }
        label: { type: string }

    Corner:
      type: string
      enum: [nw, ne, sw, se]
"""

private val parametersStyleQuery: String = """# How a query parameter is laid out on the wire.
#
# OpenAPI 4.8.11 defines six styles plus `deepObject`, and the generator read none of them: every
# array went out as a repeated key whatever the document said. That is right for the default and
# wrong for everything else - `explode: false` means one key holding `a,b,c`, and a client sending
# three keys for it is sending something the server will not understand.
#
# Ktor's resources format can bind a primitive, an enum and a `List<primitive>` from a repeated key,
# and nothing else. So the parameters below split in two: the ones it can bind stay properties of
# the `@Resource` class, and the rest are read off the call in the route body.

openapi: 3.0.3

info:
  title: styles
  version: 1.0.0

paths:
  /v1/search:
    get:
      operationId: search
      parameters:
        # The default. Stays on the resource, and keeps the inline encoding the client always had.
        - name: tag
          in: query
          schema:
            type: array
            items: { type: string }

        # One key holding `a,b`. Ktor hands back `listOf("a,b")`, so the route splits it.
        - name: ids
          in: query
          style: form
          explode: false
          schema:
            type: array
            items: { type: string }

        # One key holding `a b`.
        - name: spaced
          in: query
          style: spaceDelimited
          explode: false
          schema:
            type: array
            items: { type: string }

        # One key holding `a|b`.
        - name: piped
          in: query
          style: pipeDelimited
          explode: false
          schema:
            type: array
            items: { type: string }

        # `filter[colour]=red&filter[size]=xl`. The key is not the property name, so the resources
        # format cannot bind it at all.
        - name: filter
          in: query
          style: deepObject
          explode: true
          schema: { ${'$'}ref: "#/components/schemas/Filter" }

        # `colour=red&size=xl`: one key per field, named by the field alone.
        - name: exploded
          in: query
          style: form
          explode: true
          schema: { ${'$'}ref: "#/components/schemas/Filter" }

        # `where=colour,red,size,xl`.
        - name: joined
          in: query
          style: form
          explode: false
          schema: { ${'$'}ref: "#/components/schemas/Filter" }

        # A path-shaped value, where a `/` means a `/`.
        - name: prefix
          in: query
          allowReserved: true
          schema: { type: string }

      responses:
        "200":
          description: results
          content:
            application/json:
              schema:
                type: array
                items: { type: string }

components:
  schemas:
    Filter:
      type: object
      properties:
        colour: { type: string }
        size: { type: string }
"""

private val petstore: String = """openapi: 3.1.0
info:
  title: Pet Store
  version: 2.0.0
servers:
  - url: https://pets.example.com/v2
security:
  - bearer: []
paths:
  /pets:
    get:
      operationId: listPets
      tags: [pets]
      x-kapi-timeout: { request: PT5S, connect: PT1S }
      security: []
      parameters:
        - { name: status, in: query, schema: { ${'$'}ref: '#/components/schemas/Status' } }
        - { name: tags, in: query, schema: { type: array, items: { type: string } } }
        - { name: limit, in: query, schema: { type: integer, format: int32, default: 20, minimum: 1, maximum: 100 } }
        - { name: X-Trace, in: header, schema: { type: string } }
      responses:
        '200':
          description: The pets
          headers:
            X-Total: { schema: { type: integer } }
          content:
            application/json:
              schema: { type: array, items: { ${'$'}ref: '#/components/schemas/Pet' } }
    post:
      operationId: createPet
      tags: [pets]
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: '#/components/schemas/NewPet' }
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema: { ${'$'}ref: '#/components/schemas/Pet' }
        '400':
          description: Invalid
          content:
            application/problem+json:
              schema: { ${'$'}ref: '#/components/schemas/Problem' }
  /pets/{petId}:
    parameters:
      - { name: petId, in: path, required: true, schema: { type: integer, format: int64 } }
    get:
      operationId: getPet
      tags: [pets]
      responses:
        '200':
          description: The pet
          headers:
            ETag: { schema: { type: string } }
          content:
            application/json:
              schema: { ${'$'}ref: '#/components/schemas/Pet' }
        '404':
          description: Not found
          content:
            application/problem+json:
              schema: { ${'$'}ref: '#/components/schemas/Problem' }
    patch:
      operationId: patchPet
      tags: [pets]
      requestBody:
        required: true
        content:
          application/merge-patch+json:
            schema: { ${'$'}ref: '#/components/schemas/NewPet' }
      responses:
        '200':
          description: The patched pet
          content:
            application/json:
              schema: { ${'$'}ref: '#/components/schemas/Pet' }
    delete:
      operationId: deletePet
      tags: [pets]
      responses:
        '204': { description: Gone }
        default:
          description: Failure
          content:
            application/json:
              schema: { ${'$'}ref: '#/components/schemas/Problem' }
  /pets/{petId}/photo:
    parameters:
      - { name: petId, in: path, required: true, schema: { type: integer, format: int64 } }
    get:
      operationId: downloadPhoto
      tags: [media]
      responses:
        '200':
          description: The photo
          headers:
            Content-Disposition: { schema: { type: string } }
          content:
            application/octet-stream:
              schema: { type: string, format: binary }
        '206':
          description: Part of the photo
          content:
            application/octet-stream:
              schema: { type: string, format: binary }
        '404':
          description: No photo
    put:
      operationId: uploadPhoto
      tags: [media]
      requestBody:
        required: true
        content:
          application/octet-stream:
            schema: { type: string, format: binary }
      responses:
        '200':
          description: Size
          content:
            text/plain:
              schema: { type: string }
  /pets/{petId}/subscriptions:
    parameters:
      - { name: petId, in: path, required: true, schema: { type: integer, format: int64 } }
    post:
      operationId: subscribe
      tags: [subscriptions]
      security: []
      parameters:
        - { name: via, in: query, schema: { type: string } }
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [callbackUrl]
              properties:
                callbackUrl: { type: string }
      responses:
        '201': { description: Subscribed }
      callbacks:
        petChanged:
          '{${'$'}request.body#/callbackUrl}?pet={${'$'}request.path.petId}&via={${'$'}request.query.via}':
            post:
              operationId: petChanged
              requestBody:
                required: true
                content:
                  application/json:
                    schema: { ${'$'}ref: '#/components/schemas/Pet' }
              responses:
                '204': { description: Taken }
  /catalog/cursor:
    get:
      operationId: cursorNames
      tags: [catalog]
      security: []
      x-kapi-pagination: { style: cursor, parameter: cursor, items: items, next: next }
      parameters:
        - { name: cursor, in: query, schema: { type: string } }
      responses:
        '200':
          description: A page
          content:
            application/json:
              schema:
                type: object
                required: [items]
                properties:
                  items: { type: array, items: { type: string } }
                  next: { type: string }
  /catalog/offset:
    get:
      operationId: offsetNames
      tags: [catalog]
      security: []
      x-kapi-pagination: { style: offset, parameter: offset, size: limit }
      parameters:
        - { name: offset, in: query, schema: { type: integer, format: int32, default: 0 } }
        - { name: limit, in: query, schema: { type: integer, format: int32 } }
      responses:
        '200':
          description: A page
          content:
            application/json:
              schema: { type: array, items: { type: string } }
  /catalog/pages:
    get:
      operationId: pagedNames
      tags: [catalog]
      security: []
      x-kapi-pagination: { style: page, parameter: page, items: items }
      parameters:
        - { name: page, in: query, schema: { type: integer, format: int64, minimum: 1 } }
      responses:
        '200':
          description: A page
          content:
            application/json:
              schema:
                type: object
                properties:
                  items: { type: array, items: { type: string } }
  /catalog/linked:
    get:
      operationId: linkedNames
      tags: [catalog]
      security: []
      x-kapi-pagination: { style: link, items: items }
      parameters:
        - { name: from, in: query, schema: { type: integer, format: int32 } }
      responses:
        '200':
          description: A page
          content:
            application/json:
              schema:
                type: object
                required: [items]
                properties:
                  items: { type: array, items: { type: string } }
  /account:
    get:
      operationId: whoAmI
      tags: [account]
      security:
        - oauth: ['account:read']
        - apiKey: []
      responses:
        '200':
          description: The caller
          content:
            text/plain:
              schema: { type: string }
  /pets/{petId}/events:
    parameters:
      - { name: petId, in: path, required: true, schema: { type: integer, format: int64 } }
    get:
      operationId: petEvents
      tags: [streams]
      security: []
      responses:
        '200':
          description: One event per change of the pet
          content:
            text/event-stream:
              schema: { ${'$'}ref: '#/components/schemas/Pet' }
  /export:
    get:
      operationId: exportPets
      tags: [streams]
      security: []
      responses:
        '200':
          description: Every pet, one per line
          content:
            application/x-ndjson:
              schema: { type: array, items: { ${'$'}ref: '#/components/schemas/Pet' } }
              example:
                - { id: 7, name: Rex, status: available, kind: { type: dog, good: true } }
                - { id: 8, name: Tom, status: sold, kind: { type: cat, lives: 9 } }
  /notes{scope}:
    get:
      operationId: listNotes
      tags: [streams]
      security: []
      parameters:
        - { name: scope, in: path, required: true, style: matrix, schema: { type: string } }
      responses:
        '200':
          description: The notes of a scope, one per event
          content:
            text/event-stream:
              schema: { type: string }
  /admin/reset:
    post:
      operationId: resetStore
      tags: [admin]
      security:
        - bearer: []
          apiKey: []
      responses:
        '204': { description: Reset }
  /pets/{petId}/album:
    parameters:
      - { name: petId, in: path, required: true, schema: { type: integer, format: int64 } }
    post:
      operationId: uploadAlbum
      tags: [media]
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              type: object
              required: [photos, meta]
              properties:
                photos: { type: array, items: { type: string, format: binary } }
                labels: { type: array, items: { type: string } }
                meta:
                  type: object
                  required: [title]
                  properties:
                    title: { type: string }
            encoding:
              photos: { contentType: image/png }
      responses:
        '200':
          description: Summary
          content:
            text/plain:
              schema: { type: string }
  /pets/{petId}/card:
    parameters:
      - { name: petId, in: path, required: true, schema: { type: integer, format: int64 } }
    get:
      operationId: petCard
      tags: [formats]
      security: []
      responses:
        '200':
          description: The pet as JSON, CBOR or CSV
          content:
            application/json: { schema: { ${'$'}ref: '#/components/schemas/Pet' } }
            application/cbor: { schema: { ${'$'}ref: '#/components/schemas/Pet' } }
            text/csv: { schema: { type: string } }
  /pets/import:
    post:
      operationId: importPets
      tags: [formats]
      security: []
      requestBody:
        required: true
        content:
          application/json: { schema: { type: array, items: { ${'$'}ref: '#/components/schemas/NewPet' } } }
          application/xml: { schema: { type: array, items: { ${'$'}ref: '#/components/schemas/NewPet' } } }
      responses:
        '200':
          description: Imported
          content:
            text/plain: { schema: { type: string } }
  /pets/{petId}/notes:
    parameters:
      - { name: petId, in: path, required: true, schema: { type: integer, format: int64 } }
    put:
      operationId: putNote
      tags: [formats]
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [text]
              properties:
                text: { type: string }
          text/plain: { schema: { type: string } }
      responses:
        '200':
          description: Stored
          content:
            text/plain: { schema: { type: string } }
  /health:
    servers:
      - url: /ops
        description: operations
    get:
      operationId: health
      tags: [ops]
      security: []
      responses:
        '200':
          description: Healthy
          content:
            text/plain:
              schema: { type: string }
  /login:
    post:
      operationId: login
      tags: [auth]
      security: []
      requestBody:
        required: true
        content:
          application/x-www-form-urlencoded:
            schema:
              type: object
              required: [user, password]
              properties:
                user: { type: string }
                password: { type: string, x-kapi-sensitive: true }
                scopes: { type: array, items: { type: string } }
                device:
                  type: object
                  properties:
                    name: { type: string }
      responses:
        '200':
          description: Token
          content:
            application/json:
              schema:
                type: object
                required: [token]
                properties:
                  token: { type: string }
webhooks:
  petAdopted:
    post:
      operationId: petAdopted
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: '#/components/schemas/Pet' }
      responses:
        '202': { description: Noted }
components:
  securitySchemes:
    bearer: { type: http, scheme: bearer }
    apiKey: { type: apiKey, name: X-Api-Key, in: header }
    oauth:
      type: oauth2
      flows:
        clientCredentials:
          tokenUrl: /oauth/token
          scopes: { 'account:read': Read the own account }
  schemas:
    Status: { type: string, enum: [available, pending, sold] }
    NewPet:
      type: object
      required: [name, kind]
      properties:
        name: { type: string, minLength: 1, maxLength: 20 }
        kind: { ${'$'}ref: '#/components/schemas/Kind' }
        tags: { type: array, items: { type: string }, maxItems: 3 }
        born: { type: string, format: date }
    Pet:
      allOf:
        - ${'$'}ref: '#/components/schemas/NewPet'
        - type: object
          required: [id, status]
          properties:
            id: { type: integer, format: int64 }
            status: { ${'$'}ref: '#/components/schemas/Status' }
            seen: { type: string, format: date-time }
            key: { type: string, format: uuid }
    Kind:
      oneOf:
        - ${'$'}ref: '#/components/schemas/Cat'
        - ${'$'}ref: '#/components/schemas/Dog'
      discriminator:
        propertyName: type
        mapping: { cat: '#/components/schemas/Cat', dog: '#/components/schemas/Dog' }
    Cat:
      type: object
      required: [type, lives]
      properties:
        type: { type: string }
        lives: { type: integer, minimum: 0, maximum: 9 }
    Dog:
      type: object
      required: [type]
      properties:
        type: { type: string }
        good: { type: boolean, default: true }
    Problem:
      type: object
      required: [title]
      properties:
        title: { type: string }
        status: { type: integer }
        detail: { type: string }
"""

private val readwriteSplit: String = """# `readOnly` and `writeOnly`.
#
# Both only made a property optional, and `excludeWriteOnly` had been declared in the generator for
# a long time without anything ever setting it. So a document saying "the server assigns the id and
# a client may not send it" got one class carrying both, and nothing stopped a caller from sending
# the id or a server from echoing the password back.
#
# Two classes are emitted only where the document has earned them: the schema travels **both** ways
# and marks something. `Note` below travels both ways and marks nothing, so it stays one class -
# which is the rule that keeps the model count from doubling for a document like OpenBao's, whose
# 592 operations declare neither keyword.

openapi: 3.0.3

info:
  title: read-write
  version: 1.0.0

paths:
  # `Account` goes out as a request body and comes back as a response, so it is the case that splits.
  /v1/accounts:
    post:
      operationId: createAccount
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/Account" }
      responses:
        "201":
          description: created
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Account" }

  # `AuditEntry` only ever comes back. It marks a property `readOnly`, but there is no request shape
  # to contrast with, so one class is the whole story.
  /v1/audit:
    get:
      operationId: listAudit
      responses:
        "200":
          description: entries
          content:
            application/json:
              schema:
                type: array
                items: { ${'$'}ref: "#/components/schemas/AuditEntry" }

  # `Note` travels both ways and declares neither keyword, so it stays one class.
  /v1/notes:
    put:
      operationId: putNote
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/Note" }
      responses:
        "200":
          description: stored
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Note" }

components:
  schemas:
    Account:
      type: object
      required: [email]
      properties:
        # The server assigns it, so a request has no business carrying one.
        id:
          type: string
          readOnly: true
        email:
          type: string
          format: email
        # A client sends it and the server must never send it back.
        password:
          type: string
          writeOnly: true
        createdAt:
          type: string
          format: date-time
          readOnly: true

    AuditEntry:
      type: object
      required: [at]
      properties:
        at:
          type: string
          format: date-time
          readOnly: true
        actor: { type: string }

    Note:
      type: object
      required: [text]
      properties:
        text: { type: string }
      # The part of a document people actually read, and the part nothing checked until now.
      example:
        text: remember the milk
"""

private val readwrite: String = """# `readOnly` and `writeOnly`.
#
# Both only made a property optional, and `excludeWriteOnly` had been declared in the generator for
# a long time without anything ever setting it. So a document saying "the server assigns the id and
# a client may not send it" got one class carrying both, and nothing stopped a caller from sending
# the id or a server from echoing the password back.
#
# Two classes are emitted only where the document has earned them: the schema travels **both** ways
# and marks something. `Note` below travels both ways and marks nothing, so it stays one class -
# which is the rule that keeps the model count from doubling for a document like OpenBao's, whose
# 592 operations declare neither keyword.

openapi: 3.0.3

info:
  title: read-write
  version: 1.0.0

paths:
  # `Account` goes out as a request body and comes back as a response, so it is the case that splits.
  /v1/accounts:
    post:
      operationId: createAccount
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/Account" }
      responses:
        "201":
          description: created
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Account" }

  # `AuditEntry` only ever comes back. It marks a property `readOnly`, but there is no request shape
  # to contrast with, so one class is the whole story.
  /v1/audit:
    get:
      operationId: listAudit
      responses:
        "200":
          description: entries
          content:
            application/json:
              schema:
                type: array
                items: { ${'$'}ref: "#/components/schemas/AuditEntry" }

  # `Note` travels both ways and declares neither keyword, so it stays one class.
  /v1/notes:
    put:
      operationId: putNote
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/Note" }
      responses:
        "200":
          description: stored
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Note" }

components:
  schemas:
    Account:
      type: object
      required: [email]
      properties:
        # The server assigns it, so a request has no business carrying one.
        id:
          type: string
          readOnly: true
        email:
          type: string
          format: email
        # A client sends it and the server must never send it back.
        password:
          type: string
          writeOnly: true
        createdAt:
          type: string
          format: date-time
          readOnly: true

    AuditEntry:
      type: object
      required: [at]
      properties:
        at:
          type: string
          format: date-time
          readOnly: true
        actor: { type: string }

    Note:
      type: object
      required: [text]
      properties:
        text: { type: string }
      example:
        text: remember the milk
"""

private val responseLinks: String = """# Where a document says a caller can go next.
#
# A `links` entry is the document stating a connection it already knows about: this answer carries an
# id, and here is the operation that takes one. The generator read none of it, so every caller wrote
# `client.getJob(id = answer.body.id)` by hand and the statement in the document was decoration.
#
# Only the mechanical subset is generated. The first link below is it: every parameter comes from a
# property of the body that was just received, that property is required, and the target needs
# nothing else. The three after it are each one reason the generator refuses, and are here so that
# the refusals are exercised rather than assumed.

openapi: 3.0.3

info:
  title: links
  version: 1.0.0

paths:
  /v1/jobs:
    post:
      operationId: createJob
      requestBody:
        required: true
        content:
          application/json:
            schema: { ${'$'}ref: "#/components/schemas/NewJob" }
      responses:
        "202":
          description: accepted
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Job" }
          links:
            # The whole of what is generated.
            jobStatus:
              operationId: getJob
              parameters:
                id: "${'$'}response.body#/id"

            # `owner` is not required by the schema, and `listJobsFor` requires the parameter it
            # would fill. Generating it would mean handing a `String?` to a `String`.
            ownersJobs:
              operationId: listJobsFor
              parameters:
                owner: "${'$'}response.body#/owner"

            # Not a property of the body at all. There is no evaluating this in a build.
            fromHeader:
              operationId: getJob
              parameters:
                id: "${'$'}response.header.Location"

            # An operation this document does not declare, which `openapiLint` also refuses.
            elsewhere:
              operationId: getSomethingElse
              parameters:
                id: "${'$'}response.body#/id"

  /v1/jobs/{id}:
    get:
      operationId: getJob
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: string }
      responses:
        "200":
          description: the job
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/Job" }
        "404":
          description: no such job

  /v1/owners/{owner}/jobs:
    get:
      operationId: listJobsFor
      parameters:
        - name: owner
          in: path
          required: true
          schema: { type: string }
      responses:
        "200":
          description: their jobs
          content:
            application/json:
              schema:
                type: array
                items: { ${'$'}ref: "#/components/schemas/Job" }

components:
  schemas:
    NewJob:
      type: object
      required: [name]
      properties:
        name: { type: string }

    Job:
      type: object
      required: [id]
      properties:
        id: { type: string }
        owner: { type: string }
"""

private val responsesDefault: String = """# The `default` response.
#
# `default` is what an operation answers with when nothing more specific applies, and the status is
# something the caller learns at run time - the same shape a range like `4XX` has. It used to be
# filtered out before the sealed answer type was built, so an operation whose only error branch was
# `default` generated no error branch at all. That is the shape of every Ory document: 106 of their
# operations declare a `200` and a `default` and nothing else.

openapi: 3.0.3

info:
  title: defaults
  version: 1.0.0

paths:
  # The Ory shape: one success, and `default` for everything else.
  /v1/things:
    get:
      operationId: listThings
      responses:
        "200":
          description: the things
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/ThingList" }
        default:
          description: an error
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/GenericError" }

  # `default` beside explicit codes and a range, so that all three coexist in one sealed type and
  # none of them shadows another.
  /v1/things/{id}:
    parameters:
      - { name: id, in: path, required: true, schema: { type: string } }
    delete:
      operationId: deleteThing
      responses:
        "204": { description: gone }
        "404": { description: never existed }
        "5XX":
          description: broken
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/GenericError" }
        default:
          description: anything else
          content:
            application/json:
              schema: { ${'$'}ref: "#/components/schemas/GenericError" }

  # `default` alone, carrying no body: the answer is a status and nothing more.
  /v1/ping:
    get:
      operationId: ping
      responses:
        default: { description: whatever happened }

components:
  schemas:
    ThingList:
      type: object
      required: [things]
      properties:
        things:
          type: array
          items: { type: string }

    GenericError:
      type: object
      required: [message]
      properties:
        message: { type: string }
        code: { type: integer, format: int64 }
"""

private const val securityAndOr: String = """# What a document says a caller must bring.
#
# `security` is an **or of ands**: the array holds alternatives, and each element holds schemes that
# all apply. The generator used to collapse that to four values - none, required, prohibited,
# optional - which threw away the scopes, threw away the difference between "either of these" and
# "both of these", and emitted `authenticate("a", "b")` for an operation asking for both. Ktor reads
# that as *or*, so the route was half as demanding as the document said.
#
# Nothing generated a client credential at all: a caller had to know out of band that this API wants
# a bearer token, and put it in `customHeaders` by hand.

openapi: 3.0.3

info:
  title: security
  version: 1.0.0

# The default for every operation that says nothing.
security:
  - bearerAuth: []

paths:
  # One scheme, the ordinary case.
  /v1/things:
    get:
      operationId: listThings
      responses:
        "200": { description: ok }

  # Both at once. This is the case the flat form got wrong.
  /v1/admin:
    get:
      operationId: readAdmin
      security:
        - apiKeyAuth: []
          bearerAuth: []
      responses:
        "200": { description: ok }

  # Either one. Ktor's variadic `authenticate` is exactly this.
  /v1/either:
    get:
      operationId: readEither
      security:
        - apiKeyAuth: []
        - basicAuth: []
      responses:
        "200": { description: ok }

  # Scopes, which nothing carried before.
  /v1/scoped:
    get:
      operationId: readScoped
      security:
        - oauth2: [read:things, write:things]
      responses:
        "200": { description: ok }

  # `security: []` is an operation saying it wants nothing, which is not the same as saying nothing
  # at all - that would inherit the document's default.
  /v1/open:
    get:
      operationId: readOpen
      security: []
      responses:
        "200": { description: ok }

  # An alternative that permits no credentials at all, beside one that asks for some.
  /v1/optional:
    get:
      operationId: readOptional
      security:
        - {}
        - bearerAuth: []
      responses:
        "200": { description: ok }

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

    basicAuth:
      type: http
      scheme: basic

    apiKeyAuth:
      type: apiKey
      in: header
      name: X-Api-Key

    oauth2:
      type: oauth2
      flows:
        clientCredentials:
          tokenUrl: https://example.test/oauth2/token
          scopes:
            read:things: read them
            write:things: change them
"""

private const val security: String = """# What a document says a caller must bring.
#
# `security` is an **or of ands**: the array holds alternatives, and each element holds schemes that
# all apply. The generator used to collapse that to four values - none, required, prohibited,
# optional - which threw away the scopes, threw away the difference between "either of these" and
# "both of these", and emitted `authenticate("a", "b")` for an operation asking for both. Ktor reads
# that as *or*, so the route was half as demanding as the document said.
#
# Nothing generated a client credential at all: a caller had to know out of band that this API wants
# a bearer token, and put it in `customHeaders` by hand.

openapi: 3.0.3

info:
  title: security
  version: 1.0.0

# The default for every operation that says nothing.
security:
  - bearerAuth: []

paths:
  # One scheme, the ordinary case.
  /v1/things:
    get:
      operationId: listThings
      responses:
        "200": { description: ok }

  # Both at once. This is the case the flat form got wrong.
  /v1/admin:
    get:
      operationId: readAdmin
      security:
        - apiKeyAuth: []
          bearerAuth: []
      responses:
        "200": { description: ok }

  # Either one. Ktor's variadic `authenticate` is exactly this.
  /v1/either:
    get:
      operationId: readEither
      security:
        - apiKeyAuth: []
        - basicAuth: []
      responses:
        "200": { description: ok }

  # Scopes, which nothing carried before.
  /v1/scoped:
    get:
      operationId: readScoped
      security:
        - oauth2: [read:things, write:things]
      responses:
        "200": { description: ok }

  # `security: []` is an operation saying it wants nothing, which is not the same as saying nothing
  # at all - that would inherit the document's default.
  /v1/open:
    get:
      operationId: readOpen
      security: []
      responses:
        "200": { description: ok }

  # An alternative that permits no credentials at all, beside one that asks for some.
  /v1/optional:
    get:
      operationId: readOptional
      security:
        - {}
        - bearerAuth: []
      responses:
        "200": { description: ok }

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

    basicAuth:
      type: http
      scheme: basic

    apiKeyAuth:
      type: apiKey
      in: header
      name: X-Api-Key

    oauth2:
      type: oauth2
      flows:
        clientCredentials:
          tokenUrl: https://example.test/oauth2/token
          scopes:
            read:things: read them
            write:things: change them
"""

private val styles: String = """# How a query parameter is laid out on the wire.
#
# OpenAPI 4.8.11 defines six styles plus `deepObject`, and the generator read none of them: every
# array went out as a repeated key whatever the document said. That is right for the default and
# wrong for everything else - `explode: false` means one key holding `a,b,c`, and a client sending
# three keys for it is sending something the server will not understand.
#
# Ktor's resources format can bind a primitive, an enum and a `List<primitive>` from a repeated key,
# and nothing else. So the parameters below split in two: the ones it can bind stay properties of
# the `@Resource` class, and the rest are read off the call in the route body.
#
# Path parameters get the same treatment for a different reason. `label` and `matrix` put a prefix
# in front of the value - `.red`, `;colour=red` - and replace the placeholder rather than filling it
# in, and Ktor's route matcher has no way to say "a segment beginning with a dot". So the property
# is the raw segment and the route takes it apart.
#
# `Filter.count` is an integer on purpose. Everything read off the call arrives as text, and a
# generated signature saying `Int` is a signature the route has to make true.

openapi: 3.0.3

info:
  title: styles
  version: 1.0.0

paths:
  /v1/search:
    get:
      operationId: search
      parameters:
        # The default. Stays on the resource, and keeps the inline encoding the client always had.
        - name: tag
          in: query
          schema:
            type: array
            items: { type: string }

        # One key holding `a,b`. Ktor hands back `listOf("a,b")`, so the route splits it.
        - name: ids
          in: query
          style: form
          explode: false
          schema:
            type: array
            items: { type: string }

        # One key holding `a b`.
        - name: spaced
          in: query
          style: spaceDelimited
          explode: false
          schema:
            type: array
            items: { type: string }

        # One key holding `a|b`.
        - name: piped
          in: query
          style: pipeDelimited
          explode: false
          schema:
            type: array
            items: { type: string }

        # `filter[colour]=red&filter[size]=xl`. The key is not the property name, so the resources
        # format cannot bind it at all.
        - name: filter
          in: query
          style: deepObject
          explode: true
          schema: { ${'$'}ref: "#/components/schemas/Filter" }

        # `colour=red&size=xl`: one key per field, named by the field alone.
        - name: exploded
          in: query
          style: form
          explode: true
          schema: { ${'$'}ref: "#/components/schemas/Filter" }

        # `where=colour,red,size,xl`.
        - name: joined
          in: query
          style: form
          explode: false
          schema: { ${'$'}ref: "#/components/schemas/Filter" }

        # A path-shaped value, where a `/` means a `/`.
        - name: prefix
          in: query
          allowReserved: true
          schema: { type: string }

      responses:
        "200":
          description: results
          content:
            application/json:
              schema:
                type: array
                items: { type: string }

  # `label` with one value: `/v1/points/.42`.
  /v1/points/{index}:
    get:
      operationId: getPoint
      parameters:
        - name: index
          in: path
          required: true
          style: label
          schema: { type: integer }
      responses:
        "200":
          description: the point
          content:
            application/json:
              schema: { type: string }

  # `label` with an exploded list: `/v1/traces/.a.b.c`.
  /v1/traces/{segments}:
    get:
      operationId: getTrace
      parameters:
        - name: segments
          in: path
          required: true
          style: label
          explode: true
          schema:
            type: array
            items: { type: string }
      responses:
        "200":
          description: the trace
          content:
            application/json:
              schema: { type: string }

  # `matrix` with one value: `/v1/tiles/;id=7`.
  /v1/tiles/{id}:
    get:
      operationId: getTile
      parameters:
        - name: id
          in: path
          required: true
          style: matrix
          schema: { type: integer, format: int64 }
      responses:
        "200":
          description: the tile
          content:
            application/json:
              schema: { type: string }

  # `matrix` with an exploded object: `/v1/regions/;colour=red;count=3`, where the parameter's own
  # name appears nowhere at all.
  /v1/regions/{corner}:
    get:
      operationId: getRegion
      parameters:
        - name: corner
          in: path
          required: true
          style: matrix
          explode: true
          schema: { ${'$'}ref: "#/components/schemas/Filter" }
      responses:
        "200":
          description: the region
          content:
            application/json:
              schema: { type: string }

components:
  schemas:
    Filter:
      type: object
      required: [colour]
      properties:
        colour: { type: string }
        size: { type: string }
        count: { type: integer }
"""

private val openapi32: String = """# Every field that OpenAPI 3.2 adds, in one catalog.
#
# The document names itself by `${'$'}self`, searches with the QUERY method, copies with a method of its own,
# reads the whole query string as one parameter, streams items, nests its tags and signs in on devices.

openapi: 3.2.0
${'$'}self: https://api.example.com/catalog/openapi.yaml

info:
  title: catalog
  version: 1.0.0

servers:
  - url: https://api.example.com
    name: production
    description: Live

tags:
  - name: catalog
    summary: Catalog
    kind: nav
  - name: items
    summary: Items
    parent: catalog
    kind: nav
  - name: beta
    kind: badge

paths:
  /items:
    parameters:
      - name: session
        in: cookie
        style: cookie
        schema: { type: object, properties: { id: { type: string } } }
    get:
      operationId: listItems
      tags: [items]
      parameters:
        - name: filter
          in: querystring
          content:
            application/x-www-form-urlencoded:
              schema: { type: object, properties: { name: { type: string } } }
              examples:
                byName:
                  dataValue: { name: lamp }
                  serializedValue: name=lamp
      responses:
        "200":
          description: The items
          content:
            text/event-stream:
              itemSchema: { type: object, properties: { data: { type: string } } }
            multipart/mixed:
              prefixEncoding:
                - contentType: application/json
              itemEncoding:
                contentType: multipart/form-data
                encoding:
                  file: { contentType: image/png }
    query:
      operationId: searchItems
      tags: [items, beta]
      requestBody:
        content:
          application/json:
            ${'$'}ref: "#/components/mediaTypes/Filter"
      responses:
        "200":
          summary: Matching items
          content:
            application/jsonl:
              itemSchema: { ${'$'}ref: "#/components/schemas/Item" }
    additionalOperations:
      COPY:
        operationId: copyItems
        security:
          - device: [read]
        responses:
          "204":
            summary: Copied

components:
  schemas:
    Item:
      type: object
      discriminator:
        propertyName: kind
        mapping: { lamp: Lamp }
        defaultMapping: Item
      xml: { name: item, nodeType: element }
      properties:
        kind: { type: string }
        id: { type: string, xml: { nodeType: attribute } }
    Lamp:
      type: object

  mediaTypes:
    Filter:
      schema: { type: object, properties: { name: { type: string } } }

  securitySchemes:
    device:
      type: oauth2
      oauth2MetadataUrl: https://id.example.com/.well-known/oauth-authorization-server
      flows:
        deviceAuthorization:
          deviceAuthorizationUrl: https://id.example.com/device
          tokenUrl: https://id.example.com/token
          scopes:
            read: Read the items
    legacy:
      type: apiKey
      name: X-Key
      in: header
      deprecated: true
"""

internal val openApiTexts: Map<String, String> = mapOf(
    "additional-properties" to additionalProperties,
    "callbacks-and-webhooks" to callbacksAndWebhooks,
    "content-negotiation-request" to contentNegotiationRequest,
    "feature-matrix" to featureMatrix,
    "hostile" to hostile,
    "kitchen-sink" to kitchenSink,
    "mail-routes" to mailRoutes,
    "mail" to mail,
    "neutral" to neutral,
    "openapi-32" to openapi32,
    "openobject" to openobject,
    "outbound" to outbound,
    "parameters-constraints" to parametersConstraints,
    "parameters-cookie" to parametersCookie,
    "parameters-path-styles" to parametersPathStyles,
    "parameters-style-query" to parametersStyleQuery,
    "petstore" to petstore,
    "readwrite-split" to readwriteSplit,
    "readwrite" to readwrite,
    "response-links" to responseLinks,
    "responses-default" to responsesDefault,
    "security-and-or" to securityAndOr,
    "security" to security,
    "styles" to styles,
)
