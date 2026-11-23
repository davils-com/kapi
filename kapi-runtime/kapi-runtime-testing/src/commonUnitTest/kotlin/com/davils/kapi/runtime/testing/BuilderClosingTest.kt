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

package com.davils.kapi.runtime.testing

import com.davils.kapi.runtime.testing.contract.ContractCheckBuilder
import com.davils.kapi.runtime.testing.fixture.PETSTORE
import com.davils.kapi.runtime.testing.fixture.documentOf
import com.davils.kapi.runtime.testing.mock.MockApiBuilder
import com.davils.kapi.runtime.testing.oauth.AuthorizationServerBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec

class BuilderClosingTest : FunSpec({
    context("a builder that was evaluated") {
        test("refuses every change instead of losing it") {
            val mock = MockApiBuilder()
            val server = AuthorizationServerBuilder()
            val contract = ContractCheckBuilder(documentOf(PETSTORE))
            mock.build()
            server.build()
            contract.build()

            shouldThrow<IllegalStateException> { mock.basePath = "/v2" }
            shouldThrow<IllegalStateException> { mock.get("/pets") { } }
            shouldThrow<IllegalStateException> { server.clientId = "other" }
            shouldThrow<IllegalStateException> { server.refuse("refresh_token") }
            shouldThrow<IllegalStateException> { contract.basePath = "/v2" }
            shouldThrow<IllegalStateException> { contract.limits { } }
        }
    }
})
