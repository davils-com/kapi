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

import com.davils.kapi.spec.core.compatibility.Impact
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

private const val SEND: String = "    action: send"

private const val SAID: String = "      - ${'$'}ref: '#/channels/chat/messages/said'"

private const val LEFT: String = "      - ${'$'}ref: '#/channels/chat/messages/left'"

private const val LIMIT: String = "maxLength: 100"

private const val JSON: String = "      contentType: application/json"

private const val RECEIVE: String = "    action: receive"

private val bound: String = """
    |    bindings:
    |      ws: {}
""".trimMargin()

private val secured: String = """
    |    security:
    |      - {type: userPassword}
""".trimMargin()

private val trait: String = """
    |      traits:
    |        - {contentType: text/plain}
""".trimMargin()

class OperationRulesTest : FunSpec({
    context("the operations of a document") {
        test("are matched by their keys, and an added one breaks implementations") {
            val renamed = base.edited("  say:" to "  speak:")
            val changes = (base to renamed).changes()

            changes.map(::ruleOf) shouldBe listOf("operation-removed", "operation-added")
            changes.last().provider shouldBe Impact.Breaking
        }

        test("report a new action, a new channel, new bindings and new security") {
            val received = base.edited(SEND to RECEIVE)
            val moved = base.edited("      ${'$'}ref: '#/channels/chat'" to "      ${'$'}ref: '#/channels/echo'")
            val withBindings = base.edited(SEND to SEND + "\n" + bound)
            val withSecurity = base.edited(SEND to SEND + "\n" + secured)

            (base to received).rules() shouldBe listOf("operation-action-changed")
            (base to moved).rules() shouldBe listOf("operation-channel-changed")
            (base to withBindings).rules() shouldBe listOf("operation-bindings-changed")
            (base to withSecurity).rules() shouldBe listOf("security-added")
        }
    }

    context("the messages of an operation") {
        test("that a send operation stops sending break implementations and new ones break clients") {
            val both = base.edited(SAID to SAID + "\n" + LEFT)
            val removed = (both to base).changes().single()
            val added = (base to both).changes().single()

            ruleOf(removed) shouldBe "message-removed"
            (removed.client to removed.provider) shouldBe (Impact.NonBreaking to Impact.Breaking)
            ruleOf(added) shouldBe "message-added"
            (added.client to added.provider) shouldBe (Impact.Breaking to Impact.NonBreaking)
        }

        test("that a receive operation stops accepting break clients") {
            val receiving = base.edited(SEND to RECEIVE)
            val both = receiving.edited(SAID to SAID + "\n" + LEFT)
            val removed = (both to receiving).changes().single()

            (removed.client to removed.provider) shouldBe (Impact.Breaking to Impact.NonBreaking)
        }

        test("report a new content type, also through traits and the default of the document") {
            val plain = base.edited("contentType: application/json" to "contentType: text/plain")
            val traited = base.edited(JSON to trait)
            val unset = base.edited("$JSON\n" to "")
            val defaulted = "defaultContentType: text/plain\n$unset"

            (base to plain).rules() shouldBe listOf("message-content-type-changed")
            (plain to traited).rules().shouldBeEmpty()
            (plain to defaulted).rules().shouldBeEmpty()
        }

        test("report new bindings") {
            val nested = bound.prependIndent("  ")
            val withBindings = base.edited(JSON to "$JSON\n$nested")

            (base to withBindings).rules() shouldBe listOf("message-bindings-changed")
        }

        test("compare their payloads as data the application sends or receives") {
            val shorter = base.edited(LIMIT to "maxLength: 50")
            val receiving = base.edited(SEND to RECEIVE)
            val sent = (base to shorter).changes().single()
            val received = (receiving to receiving.edited(LIMIT to "maxLength: 50")).changes().single()

            ruleOf(sent) shouldBe "max-length"
            sent.provider shouldBe Impact.Breaking
            received.client shouldBe Impact.Breaking
        }
    }
})
