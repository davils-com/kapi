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

package com.davils.kapi.spec.asyncapi.compatibility

import com.davils.kapi.spec.asyncapi.CompatibilityCodes
import com.davils.kapi.spec.core.compatibility.Impact
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

private const val SERVER: String = "    protocol: ws"

private const val SECURED: String = "    protocol: ws\n    security:\n      - {type: userPassword}"

class DocumentRulesTest : FunSpec({
    context("the same document") {
        test("is compatible with itself") {
            (base to base).rules().shouldBeEmpty()
        }
    }

    context("the servers of a document") {
        test("are matched by their keys") {
            val renamed = base.edited("  broker:" to "  relay:")

            (base to renamed).rules() shouldBe listOf("server-removed", "server-added")
            (base to renamed).changes().first().message shouldContain "\"broker\""
        }

        test("report a new address, new bindings and new security") {
            val moved = base.edited("host: broker.example.com" to "host: relay.example.com")
            val bound = base.edited(SERVER to "$SERVER\n    bindings:\n      mqtt: {clientId: kapi}")
            val secured = base.edited(SERVER to SECURED)

            (base to moved).rules() shouldBe listOf("server-address-changed")
            (base to bound).rules() shouldBe listOf("server-bindings-changed")
            (base to secured).rules() shouldBe listOf("security-added")
            (secured to base).rules() shouldBe listOf("security-removed")
            (secured to base).changes().single().provider shouldBe Impact.Breaking
        }
    }

    context("the channels of a document") {
        test("are matched by their keys") {
            (base to base.edited("  echo:" to "  ping:")).rules() shouldBe listOf("channel-removed", "channel-added")
        }

        test("report a new address and new bindings") {
            val moved = base.edited("    address: echo" to "    address: pong")
            val bound = base.edited("    address: echo" to "    address: echo\n    bindings:\n      ws: {method: GET}")

            (base to moved).rules() shouldBe listOf("channel-address-changed")
            (base to bound).rules() shouldBe listOf("channel-bindings-changed")
        }
    }

    context("the codes of the comparison") {
        test("are named once each under asyncapi.compatibility") {
            CompatibilityCodes.all.shouldNotContainDuplicates()
            CompatibilityCodes.all.forEach { code -> code.value shouldStartWith "asyncapi.compatibility." }
            CompatibilityCodes.CHANNEL_REMOVED.value shouldBe "asyncapi.compatibility.channel-removed"
        }
    }
})
