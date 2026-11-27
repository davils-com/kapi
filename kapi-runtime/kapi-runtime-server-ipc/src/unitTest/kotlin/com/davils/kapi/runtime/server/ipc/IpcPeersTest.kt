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

package com.davils.kapi.runtime.server.ipc

import com.davils.kapi.runtime.server.Kapi
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.request.PeerIdentity
import com.davils.konnect.ipc.core.access.IpcPeer
import com.davils.konnect.ipc.core.access.IpcUser
import com.davils.konnect.ipc.server.http.IpcHttpCallAttributes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.runTestApplication

private const val PROCESS: Long = 4_242L
private const val OK: Int = 200
private const val NO_IDENTITY: String = "none"
private const val ALICE: String = "alice"

private fun peerOf(peer: IpcPeer?): ApplicationPlugin<Unit> = createApplicationPlugin("FakeIpcEngine") {
    onCall { call -> peer?.let { found -> call.attributes.put(IpcHttpCallAttributes.peer, found) } }
}

private suspend fun servedIdentityOf(peer: IpcPeer?): String {
    var body = ""
    runTestApplication {
        application {
            install(peerOf(peer))
            install(IpcPeers)
            install(Kapi)
            val probe = Operation(name = "probe", method = "GET", path = "/probe") { request ->
                reply(OK) { text(request.peer.identity?.toString() ?: NO_IDENTITY) }
            }
            routing { operation(probe) }
        }
        body = client.get("/probe").bodyAsText()
    }
    return body
}

class IpcPeersTest : FunSpec({
    context("the identity of a peer") {
        test("names the Unix user or the Windows security identifier and the process") {
            IpcPeer(IpcUser.UnixUser(ALICE), PROCESS).toPeerIdentity() shouldBe PeerIdentity(ALICE, PROCESS)
            IpcPeer(IpcUser.WindowsSid.LOCAL_SYSTEM, null).toPeerIdentity() shouldBe PeerIdentity("S-1-5-18", null)
        }

        test("is unknown for a peer without a user") {
            IpcPeer.UNKNOWN.toPeerIdentity().shouldBeNull()
        }
    }

    context("the plugin") {
        test("hands the peer of a local connection to the request of the kapi server") {
            servedIdentityOf(IpcPeer(IpcUser.UnixUser(ALICE), PROCESS)) shouldBe "PeerIdentity(alice, 4242)"
        }

        test("leaves a call alone that did not come in over a local connection") {
            servedIdentityOf(null) shouldBe NO_IDENTITY
        }

        test("leaves a call alone whose peer has no user") {
            servedIdentityOf(IpcPeer.UNKNOWN) shouldBe NO_IDENTITY
        }
    }
})
