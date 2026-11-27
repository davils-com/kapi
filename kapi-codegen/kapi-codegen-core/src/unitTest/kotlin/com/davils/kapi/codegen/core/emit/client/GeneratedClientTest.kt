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

package com.davils.kapi.codegen.core.emit.client

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

private const val DOCUMENT: String = "client"

private const val TOKEN: String = "token-1"

private const val KEY: String = "key-1"

private const val BEARER: String = "bearer"

private const val API_KEY: String = "apiKey"

private const val REFUSED: String = "failed CredentialMissing"

private const val SEPARATOR: String = "--"

private val answers: List<String> = listOf(
    "listed 1:Rex, 2:Max",
    "server error 503 down",
    "other 400 bad",
    "created 3:Rex",
    "conflict",
    "found 7:Rex as \"v1\" 5",
    "not modified \"v1\"",
    "not found",
    "unexpected 500",
    "renamed Max",
    "teapot",
    "deleted",
    "token t-1",
    "uploaded",
    "uploaded",
    "avatar set",
    "searched 1:Rex",
    "queried 1:Rex",
    "paged Rex | Max",
    "streamed message:Rex | message:Max | ended",
    "streamed null:Rex | null:Max | ended",
)

private val seen: List<String> = listOf(
    "listPets open [a b, c&d] 2 t-1 [open, closed] s-1",
    "listPets null null 503 null null null",
    "listPets null null 400 null null null",
    "createPet Rex",
    "createPet Taken",
    "renamePet 7 Value(Max)",
    "renamePet 418 null",
    "deletePet 7",
    "login a b p&w= [read, write]",
    "uploadPhoto 7 rex.png image/png PNG Rex Portrait",
    "uploadPhoto 8 null image/png GIF null null",
    "setAvatar 7 RAW",
    "searchPets Rex 2 {from=1, to=9}",
    "queryPets Rex 2",
    "pagePets null 1",
    "pagePets c2 1",
    "exportPets 2026",
)

private val keyed: List<String> = listOf(
    REFUSED,
    REFUSED,
    REFUSED,
    "created 3:Rex",
    "conflict",
    "found 7:Rex as \"v1\" 5",
    "not modified \"v1\"",
    "not found",
    "unexpected 500",
    REFUSED,
    REFUSED,
    REFUSED,
    "token t-1",
    REFUSED,
    REFUSED,
    REFUSED,
    REFUSED,
    REFUSED,
    "paged $REFUSED",
    "streamed ended",
    "streamed ended",
)

private val bearer: Authorizer = Authorizer { attempt ->
    val presented = attempt.credential as? PresentedCredential.Token
    val isKnown = presented?.run { token.reveal() } == TOKEN
    val granted = Authorization.Granted(principal("caller")).takeIf { isKnown }
    granted ?: Authorization.Unknown
}

private suspend fun played(credentials: Credentials): Any? = ServedScenarios.run(
    ServedScenario(
        document = DOCUMENT,
        server = {
            authorizer(BEARER, bearer)
            authorizer(API_KEY, ApiKeyAuthorizer { key -> principal("keeper").takeIf { key.reveal() == KEY } })
        },
        client = {
            this.credentials = credentials
            isDefaultResilience = false
        },
    ),
)

class GeneratedClientTest : FunSpec({
    context("a generated client with every credential the document names, calling a generated server") {
        val all = credentials {
            scheme(BEARER) { Secret(TOKEN) }
            scheme(API_KEY) { Secret(KEY) }
        }

        test("reads every answer as the result the document declares, and the server gets what was sent") {
            played(all) shouldBe answers + SEPARATOR + seen
        }
    }

    context("a generated client with the key alone") {
        val key = credentials { scheme(API_KEY) { Secret(KEY) } }

        test("fails every call that needs the token without sending it, and is served the rest") {
            played(
                key,
            ) shouldBe keyed + SEPARATOR + listOf("createPet Rex", "createPet Taken", "login a b p&w= [read, write]")
        }
    }
})
