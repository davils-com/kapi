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

package com.davils.kapi.runtime.testing.fixture

import com.davils.kapi.spec.core.diagnostic.Diagnostic
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import io.kotest.assertions.withClue
import io.kotest.matchers.types.shouldBeInstanceOf

internal const val BASE_URL: String = "https://api.example.com/v1"

private val uri: DocumentUri = DocumentUri("memory:/api/petstore.yaml")

internal val PETSTORE: String =
    """
        |openapi: 3.2.0
        |info:
        |  title: Petstore
        |  version: 1.0.0
        |servers:
        |  - url: https://api.example.com/v1
        |paths:
        |  /pets:
        |    get:
        |      operationId: listPets
        |      parameters:
        |        - name: limit
        |          in: query
        |          schema: { type: integer, minimum: 1, maximum: 100 }
        |        - name: tags
        |          in: query
        |          schema: { type: array, items: { type: string } }
        |        - name: Accept
        |          in: header
        |          required: true
        |          schema: { type: string }
        |      responses:
        |        "200":
        |          description: The pets
        |          headers:
        |            X-Total:
        |              required: true
        |              schema: { type: integer }
        |            X-Rate:
        |              schema: { type: integer }
        |            Content-Type:
        |              required: true
        |              schema: { type: string }
        |          content:
        |            application/json:
        |              schema:
        |                type: array
        |                items: { ${'$'}ref: "#/components/schemas/Pet" }
        |        default:
        |          description: A problem
        |          content:
        |            application/problem+json:
        |              schema: { ${'$'}ref: "#/components/schemas/Problem" }
        |    post:
        |      operationId: createPet
        |      requestBody:
        |        required: true
        |        content:
        |          application/json:
        |            schema: { ${'$'}ref: "#/components/schemas/Pet" }
        |      responses:
        |        "201":
        |          description: Created
        |          content:
        |            application/json:
        |              schema: { ${'$'}ref: "#/components/schemas/Pet" }
        |  /pets/{id}:
        |    parameters:
        |      - name: id
        |        in: path
        |        required: true
        |        schema: { type: integer, minimum: 1 }
        |    get:
        |      operationId: getPet
        |      parameters:
        |        - name: X-Request-Id
        |          in: header
        |          required: true
        |          schema: { type: string, minLength: 3 }
        |      responses:
        |        "200":
        |          description: The pet
        |          content:
        |            application/json:
        |              schema: { ${'$'}ref: "#/components/schemas/Pet" }
        |        "4XX":
        |          description: A problem
        |          content:
        |            application/problem+json:
        |              schema: { ${'$'}ref: "#/components/schemas/Problem" }
        |    delete:
        |      operationId: deletePet
        |      responses:
        |        "204":
        |          description: Deleted
        |  /pets/{id}/events:
        |    parameters:
        |      - name: id
        |        in: path
        |        required: true
        |        schema: { type: integer }
        |    get:
        |      operationId: petEvents
        |      responses:
        |        "200":
        |          description: The events
        |          content:
        |            text/event-stream:
        |              schema: { type: string }
        |  /pets/records:
        |    get:
        |      operationId: petRecords
        |      responses:
        |        "200":
        |          description: The pets as records
        |          content:
        |            application/x-ndjson:
        |              itemSchema: { ${'$'}ref: "#/components/schemas/Pet" }
        |components:
        |  schemas:
        |    Pet:
        |      type: object
        |      required: [id, name]
        |      properties:
        |        id: { type: integer, minimum: 1 }
        |        name: { type: string, minLength: 1 }
        |        tag: { type: string }
        |    Problem:
        |      type: object
        |      properties:
        |        title: { type: string }
        |        status: { type: integer }
    """.trimMargin()

internal suspend fun documentOf(text: String): Document {
    val reading = OpenApi.reader().read(Source.Text(text, uri))
    return withClue(
        reading.diagnostics.map(Diagnostic::message),
    ) { reading.shouldBeInstanceOf<Reading.Read>().document }
}
