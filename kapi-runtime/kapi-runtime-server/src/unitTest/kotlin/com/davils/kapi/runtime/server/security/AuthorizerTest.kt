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

package com.davils.kapi.runtime.server.security

import com.davils.kapi.runtime.server.PeerAttributes
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.certificateOf
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.fixture.serve
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.CredentialSource
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.request.PeerIdentity
import com.davils.kore.core.principal.currentPrincipal
import com.davils.kore.core.principal.principal
import com.davils.krypt.x509.Certificate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.testing.ApplicationTestBuilder

private const val GOOD_BASIC: String = "Basic Z29vZDpwYXNz"

private const val GOOD_BEARER: String = "Bearer good"

private const val BAD_BEARER: String = "Bearer bad"

private const val OK: Int = 200

private const val UNAUTHORIZED: Int = 401

private const val FORBIDDEN: Int = 403

private const val TOKEN: String = "token"

private const val KEY_HEADER: String = "X-Api-Key"

private const val AUTHORIZATION: String = "Authorization"

private const val GOOD: String = "good"

private const val WEAK: String = "weak"

private const val CHALLENGE: String = "WWW-Authenticate"

private const val PROCESS: Long = 4_242L

private val bearer: SchemeUse = SchemeUse(TOKEN, CredentialSource.Bearer, setOf("notes:write"))

private val header: SchemeUse = SchemeUse("key", CredentialSource.Header(KEY_HEADER))

private val tls: SchemeUse = SchemeUse("tls", CredentialSource.Certificate)

private fun callerOf(credential: PresentedCredential): String = when (credential) {
    is PresentedCredential.Token -> {
        credential.token.reveal()
    }

    is PresentedCredential.Key -> {
        credential.key.reveal()
    }

    is PresentedCredential.Basic -> {
        credential.user
    }

    is PresentedCredential.Certificates -> {
        val leaf = credential.chain.first()
        leaf.subject.commonName.orEmpty()
    }

    is PresentedCredential.Peer -> {
        credential.identity.user
    }
}

private val trusting: Authorizer = Authorizer { attempt ->
    val caller = callerOf(attempt.credential)
    val granted = Authorization.Granted(principal("${attempt.scheme}:${attempt.scopes.sorted()}"))
    val decided = granted.takeIf { caller == GOOD } ?: Authorization.Forbidden(attempt.scopes).takeIf { caller == WEAK }
    decided ?: Authorization.InvalidToken
}

private val schemes: List<String> = listOf(TOKEN, "key", "tls", "basic", "query", "cookie")

private fun guarded(security: List<List<SchemeUse>>, seen: MutableList<String?> = mutableListOf()): Operation =
    Operation(name = "guarded", method = "GET", path = PROBE, security = security) { request ->
        seen += currentPrincipal()?.id
        seen += request.principal?.id
        reply(OK)
    }

private suspend fun served(
    security: List<List<SchemeUse>>,
    authorizer: Authorizer? = trusting,
    transport: Application.() -> Unit = {},
    block: suspend ApplicationTestBuilder.() -> Unit,
) {
    serve(
        settings = { authorizer?.let { found -> schemes.forEach { scheme -> authorizer(scheme, found) } } },
        transport = transport,
        routes = { operation(guarded(security)) },
        block = block,
    )
}

private suspend fun ApplicationTestBuilder.call(
    vararg headers: Pair<String, String>,
    path: String = PROBE,
): HttpResponse = call(headers.toList(), path)

private suspend fun ApplicationTestBuilder.call(
    headers: List<Pair<String, String>>,
    path: String = PROBE,
): HttpResponse = client.get(path) { headers.forEach { (name, value) -> header(key = name, value = value) } }

private fun Application.presenting(chain: List<Certificate>?, identity: PeerIdentity? = null) {
    val transport = createApplicationPlugin("transport") {
        onCall { call ->
            chain?.let { certificates -> call.attributes.put(PeerAttributes.certificates, certificates) }
            identity?.let { peer -> call.attributes.put(PeerAttributes.identity, peer) }
        }
    }
    install(transport)
}

class AuthorizerTest : FunSpec({
    context("the credentials of a request") {
        mapOf(
            CredentialSource.Bearer to listOf(AUTHORIZATION to "bearer good"),
            CredentialSource.Basic to listOf(AUTHORIZATION to GOOD_BASIC),
            CredentialSource.Header(KEY_HEADER) to listOf("x-api-key" to GOOD),
            CredentialSource.Cookie("sid") to listOf("Cookie" to "sid=good"),
        ).forEach { (source, headers) ->
            test("are taken from where the scheme places them: $source") {
                served(listOf(listOf(SchemeUse(TOKEN, source)))) {
                    call(headers).status.value shouldBe OK
                    call().status.value shouldBe UNAUTHORIZED
                }
            }
        }

        test("are taken from the query when the scheme places them there, once") {
            served(listOf(listOf(SchemeUse(TOKEN, CredentialSource.Query("key"))))) {
                call(path = "$PROBE?key=good").status.value shouldBe OK
                call(path = "$PROBE?key=good&key=good").status.value shouldBe UNAUTHORIZED
            }
        }

        test("reach the handler as the principal of the caller, with the scopes the operation asks for") {
            val seen = mutableListOf<String?>()
            serve(
                settings = { authorizer(TOKEN, trusting) },
                routes = { operation(guarded(listOf(listOf(bearer)), seen)) },
            ) {
                call(AUTHORIZATION to GOOD_BEARER).status.value shouldBe OK
            }

            seen shouldBe listOf("token:[notes:write]", "token:[notes:write]")
        }

        test("are not asked for by an operation without security, whose handler has no principal") {
            val seen = mutableListOf<String?>()
            serve(routes = { operation(guarded(emptyList(), seen)) }) {
                call().status.value shouldBe OK
            }

            seen shouldBe listOf(null, null)
        }
    }

    context("a request without valid credentials") {
        test("is refused as unauthenticated with the challenge of its scheme") {
            served(listOf(listOf(bearer))) {
                val response = call()

                response.status.value shouldBe UNAUTHORIZED
                response.headers.getAll(CHALLENGE) shouldBe listOf("Bearer realm=\"api\"")
                problemOf(response).code shouldBe ServerCodes.UNAUTHENTICATED
                call(AUTHORIZATION to BAD_BEARER).headers[CHALLENGE] shouldBe
                    """Bearer realm="api", error="invalid_token", error_description="The token is not valid""""
            }
        }

        test("is refused when the credential is unknown, empty, ambiguous or no base64") {
            val basic = SchemeUse(TOKEN, CredentialSource.Basic)
            served(listOf(listOf(bearer))) {
                call(AUTHORIZATION to "Bearer  ").status.value shouldBe UNAUTHORIZED
                call(AUTHORIZATION to GOOD_BEARER, AUTHORIZATION to GOOD_BEARER).status.value shouldBe
                    UNAUTHORIZED
            }
            served(listOf(listOf(basic))) {
                val garbled = call(AUTHORIZATION to "Basic %%%")

                garbled.status.value shouldBe UNAUTHORIZED
                garbled.headers[CHALLENGE] shouldBe """Basic realm="api", charset="UTF-8""""
                call(AUTHORIZATION to "Basic Z29vZA==").status.value shouldBe UNAUTHORIZED
            }
            served(listOf(listOf(header))) {
                call(KEY_HEADER to " ").headers[CHALLENGE] shouldBe null
            }
        }

        test("is refused as forbidden with the scopes that would grant it when the caller may not do this") {
            served(listOf(listOf(bearer))) {
                val response = call(AUTHORIZATION to "Bearer weak")

                response.status.value shouldBe FORBIDDEN
                problemOf(response).code shouldBe ServerCodes.FORBIDDEN
                response.headers[CHALLENGE] shouldBe
                    """Bearer realm="api", error="insufficient_scope", scope="notes:write""""
            }
            served(listOf(listOf(header))) {
                call(KEY_HEADER to WEAK).headers[CHALLENGE] shouldBe null
            }
        }

        test("is refused by a server without an authorizer for the scheme, or whose authorizer fails") {
            served(listOf(listOf(bearer)), authorizer = null) {
                call(AUTHORIZATION to GOOD_BEARER).status.value shouldBe UNAUTHORIZED
            }
            served(listOf(listOf(bearer)), authorizer = Authorizer { error("broken") }) {
                call(AUTHORIZATION to GOOD_BEARER).status.value shouldBe UNAUTHORIZED
            }
        }
    }

    context("the alternatives of an operation") {
        test("are met by the first one whose every scheme grants") {
            val both = listOf(AUTHORIZATION to BAD_BEARER, KEY_HEADER to GOOD)

            served(listOf(listOf(bearer, header), listOf(header))) {
                call(both).status.value shouldBe OK
            }
            served(listOf(listOf(bearer, header))) {
                call(both).status.value shouldBe UNAUTHORIZED
            }
        }

        test("let a caller in without credentials when one of them asks for none") {
            served(listOf(listOf(bearer), emptyList())) {
                call().status.value shouldBe OK
            }
        }
    }

    context("an operation that requires mutual TLS") {
        test("lets in the peer whose verified certificate the authorizer grants, and refuses every other") {
            val good = listOf(certificateOf(GOOD))
            val weak = listOf(certificateOf(WEAK))
            val bad = listOf(certificateOf("bad"))

            served(listOf(listOf(tls)), transport = { presenting(good) }) { call().status.value shouldBe OK }
            served(listOf(listOf(tls)), transport = { presenting(weak) }) { call().status.value shouldBe FORBIDDEN }
            served(listOf(listOf(tls)), transport = { presenting(bad) }) { call().status.value shouldBe UNAUTHORIZED }
            served(listOf(listOf(tls)), transport = { presenting(emptyList()) }) {
                val response = call()

                response.status.value shouldBe UNAUTHORIZED
                response.headers[CHALLENGE] shouldBe null
            }
        }

        test("needs every other scheme of its alternative as well, and lets a token in as another alternative") {
            val good = listOf(certificateOf(GOOD))

            served(listOf(listOf(tls, bearer)), transport = { presenting(good) }) {
                call().status.value shouldBe UNAUTHORIZED
                call(AUTHORIZATION to GOOD_BEARER).status.value shouldBe OK
            }
            served(listOf(listOf(tls), listOf(bearer))) {
                call(AUTHORIZATION to GOOD_BEARER).status.value shouldBe OK
                call().headers[CHALLENGE] shouldBe "Bearer realm=\"api\""
            }
        }
    }

    context("a peer the transport verified") {
        val peer = PeerIdentity(GOOD, PROCESS)

        test("stands in for a credential that is missing and is offered to the authorizer of the scheme") {
            served(listOf(listOf(bearer)), transport = { presenting(chain = null, identity = peer) }) {
                call().status.value shouldBe OK
                call(AUTHORIZATION to BAD_BEARER).status.value shouldBe UNAUTHORIZED
            }
            served(listOf(listOf(bearer)), transport = { presenting(null, PeerIdentity(WEAK)) }) {
                call().status.value shouldBe FORBIDDEN
            }
        }

        test("is never offered when a basic credential was presented that cannot be read") {
            val asked = mutableListOf<PresentedCredential>()
            val recording = Authorizer { attempt ->
                asked += attempt.credential
                trusting.authorize(attempt)
            }

            served(
                listOf(listOf(SchemeUse(TOKEN, CredentialSource.Basic))),
                authorizer = recording,
                transport = { presenting(chain = null, identity = peer) },
            ) {
                call(AUTHORIZATION to "Basic %%%").status.value shouldBe UNAUTHORIZED
            }

            asked.shouldBeEmpty()
        }
    }

    context("the values of authorization") {
        test("are equal by their content and show nothing secret") {
            val granted = Authorization.Granted(principal(GOOD))

            granted shouldBe Authorization.Granted(principal(GOOD))
            granted.hashCode() shouldBe Authorization.Granted(principal(GOOD)).hashCode()
            Authorization.Denied() shouldBe Authorization.Unknown
            Authorization.InvalidToken.hashCode() shouldBe
                Authorization.Denied(error = "invalid_token", description = "The token is not valid").hashCode()
            Authorization.Forbidden(setOf("a")) shouldBe Authorization.Forbidden(setOf("a"))
            Authorization.Forbidden(setOf("a")).hashCode() shouldBe setOf("a").hashCode()
            Authorization.Unknown.toString() shouldBe "Denied(none, none)"
            Authorization.InvalidToken.toString() shouldBe "Denied(invalid_token, The token is not valid)"
            Authorization.Forbidden(setOf("a")).toString() shouldBe "Forbidden([a])"
            granted.toString() shouldBe "Granted(${principal(GOOD)})"
        }
    }

    context("a basic credential presented twice") {
        test("is refused as ambiguous") {
            served(listOf(listOf(SchemeUse(TOKEN, CredentialSource.Basic)))) {
                call(AUTHORIZATION to GOOD_BASIC, AUTHORIZATION to GOOD_BASIC).status.value shouldBe
                    UNAUTHORIZED
            }
        }
    }
})
