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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val PROTOCOL: String = "    protocol: ws"

private const val ADDRESS: String = "    address: echo"

private const val MOVED: String = "server-address-changed"

private val dangling: String = """
    |servers:
    |  broker: {${'$'}ref: '#/components/servers/Missing'}
    |channels:
    |  chat: {${'$'}ref: '#/components/channels/Missing'}
    |operations:
    |  say:
    |    action: send
    |    channel: {${'$'}ref: '#/channels/Missing'}
    |    messages:
    |      - {${'$'}ref: '#/channels/chat/messages/Missing'}
    |  hear:
    |    action: receive
""".trimMargin()

private val bindings: String = """
    |    bindings:
    |      mqtt: {clientId: kapi}
""".trimMargin()

private val secured: String = """
    |    security:
    |      - {type: userPassword}
""".trimMargin()

class EdgeComparisonTest : FunSpec({
    context("a pair of documents") {
        test("that refer to what they do not hold leave it out") {
            (dangling to dangling).rules().shouldBeEmpty()
            (dangling to dangling.edited("action: receive" to "action: send")).rules() shouldBe
                listOf("operation-action-changed")
        }

        test("report every part of the address of a server") {
            val versioned = base.edited(PROTOCOL to "$PROTOCOL\n    protocolVersion: '13'")
            val secure = base.edited(PROTOCOL to "    protocol: wss")
            val rooted = base.edited(PROTOCOL to "$PROTOCOL\n    pathname: /chat")

            (base to versioned).rules() shouldBe listOf(MOVED)
            (base to secure).rules() shouldBe listOf(MOVED)
            (base to rooted).rules() shouldBe listOf(MOVED)
        }

        test("report bindings and security that change on both sides") {
            val bound = base.edited(PROTOCOL to PROTOCOL + "\n" + bindings)
            val rebound = bound.edited("clientId: kapi" to "clientId: other")
            val withSecurity = base.edited(PROTOCOL to PROTOCOL + "\n" + secured)
            val otherSecurity = withSecurity.edited("userPassword" to "X509")

            (bound to rebound).rules() shouldBe listOf("server-bindings-changed")
            (bound to base).rules() shouldBe listOf("server-bindings-changed")
            (withSecurity to otherSecurity).rules().shouldBeEmpty()
        }

        test("report a channel that loses its address and a payload that is removed") {
            val unaddressed = base.edited(ADDRESS to "    title: Echo")
            val leaving = base.edited("messages/said'\n" to "messages/left'\n")
            val bare = leaving.edited("      payload: {type: string}" to "      name: left")

            (base to unaddressed).rules() shouldBe listOf("channel-address-changed")
            (leaving to bare).rules().shouldBeEmpty()
        }
    }
})
