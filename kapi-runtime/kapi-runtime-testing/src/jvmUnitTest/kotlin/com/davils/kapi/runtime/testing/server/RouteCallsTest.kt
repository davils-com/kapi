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

package com.davils.kapi.runtime.testing.server

import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.Routes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.request.Request
import com.davils.kapi.runtime.testing.fixture.Pet
import com.davils.kore.core.principal.currentPrincipal
import com.davils.kore.core.principal.principal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.builtins.serializer

private const val OK: Int = 200

private const val NO_CONTENT: Int = 204

private const val NOT_FOUND: Int = 404

private const val GET: String = "GET"

private const val WHO: String = "whoAmI"

private const val FORGET: String = "forget"

private const val SHOW: String = "showPet"

private const val ID: String = "petId"

private val pets: Routes = object : Routes {
    override val operations: List<Operation> = listOf(
        Operation(name = SHOW, method = GET, path = "/pets/{petId}") { request -> shown(request) },
        Operation(name = WHO, method = GET, path = "/me") {
            val id = currentPrincipal()?.id.orEmpty()
            reply(OK) { text(id) }
        },
        Operation(name = FORGET, method = "DELETE", path = "/pets") { reply(NO_CONTENT) },
        Operation(name = "watch", method = GET, path = "/events") {
            reply(OK) { events(Pet.serializer(), emptyFlow()) }
        },
        Operation(name = "fail", method = GET, path = "/fail") { error("broken") },
    )
}

private fun shown(request: Request): Reply {
    val id = request.pathParameters[ID]?.toIntOrNull() ?: return reply(status = NOT_FOUND)
    val tag = request.query.firstOrNull { (name, _) -> name == "tag" }?.second
    return reply(OK) { json(Pet.serializer(), Pet(id = id, name = "Rex", tag = tag)) }
}

class RouteCallsTest : FunSpec({
    context("a call of a route") {
        test("runs the handler of the operation with the request it is given") {
            val request = Request.of(operation = SHOW, method = GET, path = "/pets/7") {
                pathParameters = mapOf(ID to "7")
                rawQuery = "tag=good"
            }

            val reply = pets.call(SHOW, request)

            reply.status shouldBe OK
            reply.mediaType shouldBe MediaType.Json
            reply.decoded(Pet.serializer()) shouldBe Pet(id = 7, name = "Rex", tag = "good")
            reply.text() shouldBe """{"id":7,"name":"Rex","tag":"good"}"""
        }

        test("builds the request on the method and path of the operation and runs it as its principal") {
            val shown = pets.call(SHOW, path = "/pets/3") { pathParameters = mapOf(ID to "3") }
            val me = pets.call(WHO) { principal = principal("alice") }
            val anonymous = pets.call(WHO)

            shown.decoded(Pet.serializer()) shouldBe Pet(id = 3, name = "Rex")
            me.text() shouldBe "alice"
            anonymous.text() shouldBe ""
            pets.call(SHOW).status shouldBe NOT_FOUND
        }

        test("reads replies without a body or with a streamed one") {
            val empty = pets.call(FORGET)
            val streamed = pets.call("watch")

            empty.content() shouldBe ByteString()
            empty.mediaType.shouldBeNull()
            streamed.content().shouldBeNull()
            streamed.text().shouldBeNull()
            streamed.mediaType?.essence shouldBe "text/event-stream"
            reply(OK) { bytes(MediaType.OctetStream, "ab".encodeToByteString()) }.text() shouldBe "ab"
        }

        test("refuses an unknown operation and a reply that holds no such value, and passes on a failure") {
            shouldThrow<IllegalArgumentException> { pets.call("unknown") }
            shouldThrow<IllegalArgumentException> { pets.call(FORGET).decoded(Pet.serializer()) }
            shouldThrow<IllegalArgumentException> {
                reply(OK) { text("x") }.decoded(Int.serializer())
            }
            shouldThrow<IllegalStateException> { pets.call("fail") }
        }
    }
})
