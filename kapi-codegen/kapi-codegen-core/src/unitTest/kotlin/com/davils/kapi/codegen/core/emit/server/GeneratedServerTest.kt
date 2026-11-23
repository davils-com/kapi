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

package com.davils.kapi.codegen.core.emit.server

import com.davils.kapi.codegen.core.fixture.ServedScenario
import com.davils.kapi.codegen.core.fixture.ServedScenarios
import com.davils.kapi.runtime.core.security.Credentials
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.core.security.credentials
import com.davils.kapi.runtime.server.security.ApiKeyAuthorizer
import com.davils.kapi.runtime.server.security.Authorization
import com.davils.kapi.runtime.server.security.Authorizer
import com.davils.kapi.runtime.server.security.PresentedCredential
import com.davils.kore.core.principal.principal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val DOCUMENT: String = "server"

private const val WRITER: String = "writer"

private const val READ: String = "pets:read"

private const val WRITE: String = "pets:write"

private const val MISSING: String = "not found"

private const val FORBIDDEN: String = "unexpected 403"

private const val UNAUTHENTICATED: String = "unexpected 401"

private const val NO_CREDENTIALS: String = "failed CredentialMissing"

private const val BAD_LIMIT: String = "other 400 bad limit"

private const val EMPTY: String = "listed 0: "

private const val DENIED: String = "other 401 The request carries no valid credentials"

private const val INVALID: String = """unexpected 400 {"type":"about:blank","title":"The request is invalid",""" +
    """"status":400,"code":"kapi.runtime.problem.invalid","violations":[{"path":"body.name",""" +
    """"code":"kapi.runtime.constraint.not-null","message":"must not be null"}]}"""

private const val RENAME_FORBIDDEN: String = """unexpected 403 {"type":"about:blank","title":"The caller may """ +
    """not do this","status":403,"code":"kapi.server.forbidden","violations":[]}"""

private const val RENAME_DENIED: String = """unexpected 401 {"type":"about:blank","title":"The request carries """ +
    """no valid credentials","status":401,"code":"kapi.server.unauthenticated","violations":[]}"""

private const val FOUND: String = "found 1:Rex:open as \"v1\""

private val granted: Map<String, Set<String>> = mapOf(WRITER to setOf(READ, WRITE), "reader" to setOf(READ))

private val served: List<String> = listOf(
    "created 1:Rex:open",
    "conflict",
    "unexpected 400",
    "created 2:Max:closed",
    "listed 2: 1:Rex:open, 2:Max:closed",
    "listed 1: 1:Rex:open",
    BAD_LIMIT,
    FOUND,
    "not modified \"v1\"",
    FOUND,
    MISSING,
    "renamed 1:Rexy:open",
    "renamed 1:Rexy:null",
    INVALID,
    MISSING,
    "stored",
    "picture PNG",
    MISSING,
    "deleted",
    MISSING,
)

private val reading: List<String> = List(4) { FORBIDDEN } +
    listOf(EMPTY, EMPTY, BAD_LIMIT, MISSING, MISSING, MISSING, MISSING) +
    List(4) { RENAME_FORBIDDEN } +
    listOf(FORBIDDEN, MISSING, MISSING, FORBIDDEN, FORBIDDEN)

private val unknown: List<String> = List(4) { UNAUTHENTICATED } +
    listOf(DENIED, DENIED, DENIED, MISSING, MISSING, MISSING, MISSING) +
    List(4) { RENAME_DENIED } +
    List(5) { UNAUTHENTICATED }

private val anonymous: List<String> =
    List(7) { NO_CREDENTIALS } + List(4) { MISSING } + List(9) { NO_CREDENTIALS }

private fun authorizer(asked: MutableList<String>): Authorizer = Authorizer { attempt ->
    asked += attempt.scheme + " " + attempt.scopes.sorted()
    val presented = attempt.credential as? PresentedCredential.Token
    val caller = presented?.run { token.reveal() }.orEmpty()
    val held = granted[caller]
    val forbidden = Authorization.Forbidden(attempt.scopes).takeIf { held?.containsAll(attempt.scopes) == false }
    val grant = held?.let { Authorization.Granted(principal(caller)) }
    forbidden ?: grant ?: Authorization.Unknown
}

private fun tokens(token: String): Credentials = credentials { scheme("oauth") { Secret(token) } }

private suspend fun played(credentials: Credentials, asked: MutableList<String> = mutableListOf()): Any? =
    ServedScenarios.run(
        ServedScenario(
            document = DOCUMENT,
            server = {
                authorizer("oauth", authorizer(asked))
                authorizer("key", ApiKeyAuthorizer { key -> principal(WRITER).takeIf { key.reveal() == "k-1" } })
            },
            client = { this.credentials = credentials },
        ),
    )

class GeneratedServerTest : FunSpec({
    context("a generated client calling a generated server") {
        test("gets every answer the service gives, through the types both were generated with") {
            played(tokens(WRITER)) shouldBe served
        }

        test("is refused what its token may not do and served the rest") {
            played(tokens("reader")) shouldBe reading
        }

        test("is refused by the server with a token it does not know, and served what needs none") {
            played(tokens("stranger")) shouldBe unknown
        }

        test("does not even call what needs a caller when it has no credentials") {
            played(Credentials.None) shouldBe anonymous
        }

        test("makes the server ask its authorizer for the scheme and the scopes each operation requires") {
            val asked = mutableListOf<String>()

            played(tokens(WRITER), asked)

            asked.distinct() shouldBe listOf("oauth [$WRITE]", "oauth [$READ]")
        }
    }
})
