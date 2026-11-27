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

package com.davils.kapi.runtime.server.tls

import com.davils.kapi.runtime.server.Kapi
import com.davils.kapi.runtime.server.PeerAttributes
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.tls.fixture.Pki
import com.davils.krypt.authority.profile.CertificateProfile
import com.davils.krypt.tls.ClientAuthentication
import com.davils.krypt.tls.peer.PeerIdentity
import com.davils.krypt.x509.Certificate
import com.davils.krypt.x509.name.GeneralName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.runTestApplication
import javax.net.ssl.SSLSession

private const val OK: Int = 200

private const val CLIENT: String = "spiffe://example.org/client"

private fun forged(certificates: List<Certificate>): ApplicationPlugin<Unit> = createApplicationPlugin("Forged") {
    onCall { call -> call.attributes.put(PeerAttributes.certificates, certificates) }
}

private suspend fun servedChainOf(
    read: (ApplicationCall) -> SSLSession?,
    forgery: List<Certificate> = emptyList(),
): String {
    var body = ""
    runTestApplication {
        application {
            install(forged(forgery))
            install(TlsPeers) { session(read) }
            install(Kapi)
            val probe = Operation(name = "probe", method = "GET", path = "/probe") { request ->
                val subjects = request.peer.certificates.map { certificate -> certificate.subject.toString() }
                reply(OK) { text(subjects.joinToString()) }
            }
            routing { operation(probe) }
        }
        body = client.get("/probe").bodyAsText()
    }
    return body
}

class TlsPeersTest : FunSpec({
    context("a connection over mutual TLS") {
        test("hands the path krypt verified to the request, the certificate of the client first") {
            val pki = Pki.create()
            val client = pki.identity(CertificateProfile.TlsClient, GeneralName.Uri(CLIENT))
            val connection = pki.connect(client, ClientAuthentication.Required)
            val session = connection.server.session
            val verified = PeerIdentity.of(session).shouldNotBeNull()

            connection.isEstablished shouldBe true
            verified.path.first() shouldBe client.certificate
            servedChainOf(read = { session }) shouldBe verified.path.joinToString { found -> found.subject.toString() }
        }

        test("hands nothing over for a session in which krypt verified no client") {
            val pki = Pki.create()
            val connection = pki.connect(client = null, authentication = ClientAuthentication.Requested)
            val session = connection.server.session

            connection.isEstablished shouldBe true
            PeerIdentity.of(session).shouldBeNull()
            servedChainOf(read = { session }) shouldBe ""
        }
    }

    context("a call without a verified chain") {
        test("loses the certificates an earlier plugin put into it") {
            val pki = Pki.create()
            val client = pki.identity(CertificateProfile.TlsClient, GeneralName.Uri(CLIENT))

            servedChainOf(read = { null }, forgery = client.chain) shouldBe ""
        }

        test("is served without certificates when the session cannot be read") {
            servedChainOf(read = { error("The engine has no session") }) shouldBe ""
        }
    }

    context("the plugin") {
        test("cannot be installed without a way to the session") {
            shouldThrow<IllegalStateException> {
                runTestApplication {
                    application { install(TlsPeers) }
                    client.get("/")
                }
            }
        }
    }
})
