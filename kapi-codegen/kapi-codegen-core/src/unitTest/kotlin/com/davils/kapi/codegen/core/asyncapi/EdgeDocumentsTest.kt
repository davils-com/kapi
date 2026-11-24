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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kapi.codegen.core.Codegen
import com.davils.kapi.codegen.core.options.Artifact
import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.asyncapi.Reading
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val REF: String = "\$ref"

private const val WS: String = "{local: {host: localhost, protocol: ws}}"

private const val SEND: String = "{send: {action: send, channel: {$REF: '#/channels/room'}}}"

private const val TEXT: String = "{a: {payload: {type: string}}}"

private const val PLAIN: String = "{room: {address: /room, messages: $TEXT}}"

private const val TAGGED: String = "{type: object, required: [kind], properties: {kind: {const: x}}}"

private const val INLINE: String = "{type: httpApiKey, name: k, in: header}"

private const val RESERVED: String = "Sec-WebSocket-Key"

private const val STRING: String = "{type: string}"

private fun identified(kind: String): String =
    "{type: object, required: [kind, id], properties: {kind: {const: $kind}, id: {type: string}}}"

private const val CORRELATION: String = "{location: '\$message.payload#/id'}"

private const val SEND_ON_ROOM: String = "{send: {action: send, channel: {$REF: '#/channels/room'}, "

private const val UNREFERENCED: String = "codegen.asyncapi.reference.unsupported" +
    "@/channels/room/messages/a/payload/properties/b/\$ref"

private const val ASK: String = "ask: {action: send, channel: {$REF: '#/channels/room'}, " +
    "messages: [{$REF: '#/channels/room/messages/a'}], reply: {channel: {$REF: '#/channels/room'}, " +
    "messages: [{$REF: '#/channels/room/messages/b'}]}}"

private const val PREFIX: String = "codegen.asyncapi."

private const val ROOM: String = "@/channels/room"

private fun doc(channels: String, servers: String = WS, components: String = "{}", operations: String = SEND): String =
    listOf(
        "asyncapi: 3.0.0",
        "info: {title: T, version: '1'}",
        "defaultContentType: application/json",
        "servers: $servers",
        "channels: $channels",
        "operations: $operations",
        "components: $components",
    ).joinToString(separator = "\n")

private fun room(channel: String): String = "{room: {address: /room, $channel}}"

private fun secured(scheme: String): String =
    "{local: {host: localhost, protocol: ws, security: [{$REF: '#/components/securitySchemes/$scheme'}]}}"

private fun schemes(declared: String): String = "{securitySchemes: $declared}"

private val cases: Map<String, Pair<String, List<String>>> = mapOf(
    "a security scheme written inline, which has no name to configure" to (
        doc(PLAIN, servers = "{local: {host: localhost, protocol: ws, security: [$INLINE]}}") to
            listOf(PREFIX + "security.unsupported@/servers/local/security/0")
        ),
    "a security scheme of X.509 certificates, which the HTTP client of the caller presents" to (
        doc(PLAIN, servers = secured("c"), components = schemes("{c: {type: X509}}")) to emptyList()
        ),
    "an OAuth 2 scheme with the scopes it asks for" to (
        doc(
            PLAIN,
            servers = secured("o"),
            components = schemes(
                "{o: {type: oauth2, scopes: [r], " +
                    "flows: {clientCredentials: {tokenUrl: 'https://a/t', availableScopes: {r: read}}}}}",
            ),
        ) to emptyList()
        ),
    "an OpenID Connect scheme with the scopes it asks for" to (
        doc(
            PLAIN,
            servers = secured("o"),
            components = schemes(
                "{o: {type: openIdConnect, scopes: [r], " +
                    "openIdConnectUrl: 'https://a/.well-known/openid-configuration'}}",
            ),
        ) to emptyList()
        ),
    "a handshake query that is no object" to (
        doc(room("messages: $TEXT, bindings: {ws: {query: {type: string}}}")) to
            listOf(PREFIX + "handshake.unsupported$ROOM/bindings/ws/query")
        ),
    "a handshake header the WebSocket protocol owns" to (
        doc(room("messages: $TEXT, bindings: {ws: {headers: {type: object, properties: {$RESERVED: $STRING}}}}")) to
            listOf(PREFIX + "handshake.reserved$ROOM/bindings/ws/headers/properties/$RESERVED")
        ),
    "a correlation id in a header, which frames do not carry" to (
        doc(room("messages: {a: {payload: {type: string}, correlationId: {location: '\$message.header#/id'}}}")) to
            listOf(PREFIX + "correlation.unsupported$ROOM/messages/a/correlationId")
        ),
    "a payload in Avro" to (
        doc(
            room(
                "messages: {a: {payload: {schemaFormat: 'application/vnd.apache.avro;version=1.9.0', " +
                    "schema: {type: record, name: R, fields: []}}}}",
            ),
        ) to listOf(PREFIX + "schema.format$ROOM/messages/a/payload")
        ),
    "a channel without an address" to (doc("{room: {messages: $TEXT}}") to listOf(PREFIX + "channel.skipped$ROOM")),
    "a channel with only a description" to (doc(room("description: About, messages: $TEXT")) to emptyList()),
    "a reply on a channel with no messages of its own" to (
        doc(
            "{room: {address: /room, messages: $TEXT}, back: {address: /back}}",
            operations = SEND_ON_ROOM +
                "reply: {channel: {$REF: '#/channels/back'}}}}",
        ) to listOf(PREFIX + "channel.skipped@/channels/back", PREFIX + "reply.channel@/operations/send/reply")
        ),
    "two messages whose tags are the same" to (
        doc(room("messages: {a: {payload: $TAGGED}, b: {payload: $TAGGED}}")) to
            listOf(PREFIX + "message.indistinct$ROOM")
        ),
    "a message without a payload among tagged ones" to (
        doc(room("messages: {a: {payload: $TAGGED}, b: {name: b}}")) to listOf(PREFIX + "message.indistinct$ROOM")
        ),
    "components in Avro and components that resolve to nothing" to (
        doc(
            PLAIN,
            components = "{schemas: {R: {schemaFormat: 'application/vnd.apache.avro;version=1.9.0', " +
                "schema: {type: record, name: R, fields: []}}, Gone: {$REF: '#/components/schemas/None'}}}",
        ) to emptyList()
        ),
    "a request and a reply without correlation ids" to (
        doc(
            "{room: {address: /room, messages: $TEXT}, back: {address: /back, messages: {b: {payload: $STRING}}}}",
            operations = SEND_ON_ROOM +
                "reply: {channel: {$REF: '#/channels/back'}}}, listen: {action: receive, " +
                "channel: {$REF: '#/channels/back'}}}",
        ) to listOf(PREFIX + "reply.channel@/operations/send/reply")
        ),
    "a channel and a message with only a title" to (
        doc("{room: {address: /room, title: Room, messages: {a: {title: Said, payload: $STRING}}}}") to emptyList()
        ),
    "a payload whose schema format is JSON Schema by name" to (
        doc(
            room(
                "messages: {a: {payload: {schemaFormat: 'application/schema+json;version=draft-07', " +
                    "schema: $STRING}}}",
            ),
        ) to emptyList()
        ),
    "a request and its reply on one channel without correlation ids" to (
        doc(
            "{room: {address: /room, messages: {a: {payload: $STRING}, b: {payload: $STRING}}}}",
            operations = "{$ASK}",
        ) to listOf(PREFIX + "reply.unpaired$ROOM/messages/a")
        ),
    "a request and its reply with correlation ids among other messages" to (
        doc(
            "{room: {address: /room, messages: {a: {payload: ${identified("a")}, correlationId: $CORRELATION}, " +
                "b: {payload: ${identified("b")}, correlationId: $CORRELATION}, " +
                "c: {payload: {type: object, required: [kind], properties: {kind: {const: c}}}}}}}",
            operations = "{ask: {action: receive, channel: {$REF: '#/channels/room'}, " +
                "messages: [{$REF: '#/channels/room/messages/a'}], reply: {channel: {$REF: '#/channels/room'}, " +
                "messages: [{$REF: '#/channels/room/messages/b'}]}}, tell: {action: send, " +
                "channel: {$REF: '#/channels/room'}, messages: [{$REF: '#/channels/room/messages/c'}]}}",
        ) to emptyList()
        ),
    "a named security scheme of a type a handshake cannot carry" to (
        doc(PLAIN, servers = secured("u"), components = schemes("{u: {type: userPassword}}")) to
            listOf(PREFIX + "security.unsupported@/components/securitySchemes/u")
        ),
    "a bearer scheme of HTTP" to (
        doc(PLAIN, servers = secured("b"), components = schemes("{b: {type: http, scheme: bearer}}")) to emptyList()
        ),
    "an operation that sends a message of the components instead of its channel" to (
        doc(
            PLAIN,
            operations = SEND_ON_ROOM +
                "messages: [{$REF: '#/components/messages/m'}]}}",
            components = "{messages: {m: {payload: $STRING}}}",
        ) to listOf(PREFIX + "message.foreign@/operations/send/messages/0")
        ),
    "an address with a parameter the channel does not declare" to (
        doc("{room: {address: '/rooms/{id}', messages: $TEXT}}") to emptyList()
        ),
    "an address without a leading slash and names Kotlin reserves" to (
        doc(
            "{class: {address: '{fun}/x', messages: {object: {payload: $STRING}}, parameters: {fun: {}}}}",
            operations = "{send: {action: send, channel: {$REF: '#/channels/class'}}}",
        ) to emptyList()
        ),
    "a request and a reply of one message each with correlation ids" to (
        doc(
            "{room: {address: /room, messages: {a: {payload: ${identified("a")}, correlationId: $CORRELATION}}}, " +
                "back: {address: /back, messages: {b: {payload: ${identified("b")}, correlationId: $CORRELATION}}}}",
            operations = "{ask: {action: send, channel: {$REF: '#/channels/room'}, " +
                "reply: {channel: {$REF: '#/channels/room'}}}}",
        ) to listOf(PREFIX + "channel.skipped@/channels/back")
        ),
    "a payload whose models take a name twice" to (
        doc(
            room("messages: {a: {payload: {$REF: '#/components/schemas/Pet'}}}"),
            components = "{schemas: {Pet: {type: object, properties: {id: {type: integer, readOnly: true}, " +
                "name: $STRING}}, PetWrite: $STRING}}",
        ) to listOf("codegen.name.collision@")
        ),
    "a payload property whose reference names no pointer" to (
        doc(room("messages: {a: {payload: {type: object, properties: {b: {$REF: '#b'}}}}}")) to listOf(UNREFERENCED)
        ),
    "a payload property that refers to a schema in Avro" to (
        doc(
            room("messages: {a: {payload: {type: object, properties: {b: {$REF: '#/components/schemas/R'}}}}}"),
            components = "{schemas: {R: {schemaFormat: 'application/vnd.apache.avro;version=1.9.0', " +
                "schema: {type: record, name: R, fields: []}}}}",
        ) to listOf(UNREFERENCED)
        ),
    "an operation whose message reference is too short to name a channel" to (
        doc(PLAIN, operations = SEND_ON_ROOM + "messages: [{$REF: '#/a'}]}}") to
            listOf(PREFIX + "message.foreign@/operations/send/messages/0")
        ),
    "a key scheme of the old kind and a basic scheme of HTTP" to (
        doc(
            PLAIN,
            servers = "{local: {host: localhost, protocol: ws, security: [{$REF: '#/components/securitySchemes/k'}, " +
                "{$REF: '#/components/securitySchemes/h'}]}}",
            components = schemes("{k: {type: apiKey, in: user}, h: {type: http, scheme: basic}}"),
        ) to listOf(PREFIX + "security.unsupported@/components/securitySchemes/k")
        ),
    "a handshake query that is an object without properties" to (
        doc(room("messages: $TEXT, bindings: {ws: {query: {type: object}}}")) to
            listOf(PREFIX + "handshake.unsupported$ROOM/bindings/ws/query")
        ),
    "a request whose sealed side has correlated and plain messages" to (
        doc(
            "{room: {address: /room, messages: {a: {payload: ${identified("a")}, correlationId: $CORRELATION}, " +
                "b: {payload: ${identified("b")}, correlationId: $CORRELATION}, " +
                "c: {payload: {type: object, required: [kind], properties: {kind: {const: c}}}}}}}",
            operations = "{$ASK, tell: {action: send, " +
                "channel: {$REF: '#/channels/room'}, messages: [{$REF: '#/channels/room/messages/c'}]}}",
        ) to emptyList()
        ),
    "a message of XML" to (
        doc(room("messages: {a: {contentType: application/xml, payload: {type: string}}}")) to
            listOf(PREFIX + "content-type.unsupported$ROOM/messages/a")
        ),
)

class EdgeDocumentsTest : FunSpec({
    context("a document at the edge of what the generator supports") {
        cases.forEach { (name, case) ->
            val (text, expected) = case

            test("with $name is generated or refused with every finding where it is") {
                val reading = AsyncApi.reader().read(Source.Text(text, DocumentUri("file:///specs/p.yaml")))
                val invalid = reading as? Reading.Invalid
                val document = invalid?.partial ?: reading.shouldBeInstanceOf<Reading.Read>().document
                val generator = Codegen.asyncApi {
                    packageName = "com.example.p"
                    apiName = "P"
                    artifacts = setOf(Artifact.Models, Artifact.Client, Artifact.Server)
                }
                val found = generator.generate(document).diagnostics.map { diagnostic ->
                    diagnostic.code.value + "@" + diagnostic.location?.run { pointer.text }
                }

                found shouldBe expected
            }
        }
    }
})
