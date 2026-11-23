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

package com.davils.kapi.runtime.client.channel

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.ClientSettingsBuilder
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.channel.fixture.Message
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.plugins.websocket.WebSockets
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private const val APPLICATION: Int = 4_000

private const val FRAME_BYTES: Long = 1_048_576

private const val REASON: String = "a\nb"

private val spec: ChannelSpec<Message, Message> = ChannelSpec(Message.serializer(), Message.serializer())

private fun sockets(configure: ClientSettingsBuilder.() -> Unit = {}): Exchanges =
    Exchanges(configure = configure, http = {
        followRedirects = false
        install(WebSockets)
    })

private suspend fun outcomeOf(
    exchanges: Exchanges,
    call: Call = pets,
    channel: ChannelSpec<Message, Message> = spec,
): ChannelOutcome<Unit> = exchanges.withClient { client: ApiClient -> client.channel(call, channel) { } }

class ChannelValuesTest : FunSpec({
    context("the settings of a channel") {
        test("have safe defaults and refuse what is out of bounds") {
            ChannelSettings.Default.maxFrameBytes shouldBe FRAME_BYTES
            ChannelSettings.Default.toString() shouldBe
                "ChannelSettings(subprotocols=[], mediaType=application/json, maxFrameBytes=1048576, " +
                "maxMessageBytes=4194304)"
            shouldThrow<ValidationException> { channelSettings { subprotocols = listOf("a b") } }
            shouldThrow<ValidationException> { channelSettings { maxFrameBytes = 0 } }
            shouldThrow<ValidationException> { channelSettings { incomingCapacity = 0 } }
            shouldThrow<ValidationException> { channelSettings { pingInterval = 2.hours } }
            shouldThrow<ValidationException> { channelSettings { closeTimeout = Duration.ZERO } }
            channelSettings { pingInterval = Duration.ZERO }.pingInterval shouldBe Duration.ZERO
        }
    }

    context("the values of a channel") {
        test("are equal by their parts and name their kind") {
            val close = ChannelClose(code = APPLICATION, reason = REASON)
            val failed = ChannelOutcome.Failed(CallFailure.CrossOrigin)
            val correlation = Correlation<Message, Message>(sent = { _ -> null }, received = { _ -> null })

            close shouldBe ChannelClose(code = APPLICATION, reason = REASON)
            close.hashCode() shouldBe ChannelClose(code = APPLICATION, reason = REASON).hashCode()
            close.kind shouldBe CloseKind.Other
            close.toString() shouldBe "ChannelClose(4000, Other, a\\nb, isLocal=false)"
            CloseKind.of(CloseKind.TryAgainLater.code) shouldBe CloseKind.TryAgainLater
            CloseKind.of(0) shouldBe CloseKind.Other
            ChannelReply.Received("x").toString() shouldBe "Received(x)"
            ChannelReply.Received("x").hashCode() shouldBe "x".hashCode()
            listOf(ChannelReply.TimedOut, ChannelReply.Closed, ChannelReply.Refused).map(Any::toString) shouldBe
                listOf("TimedOut", "Closed", "Refused")
            failed.toString() shouldBe "Failed(CrossOrigin)"
            failed.hashCode() shouldBe CallFailure.CrossOrigin.hashCode()
            failed shouldNotBe ChannelOutcome.Failed(CallFailure.TimedOut)
            ChannelOutcome.Completed(1, close).hashCode() shouldBe close.hashCode()
            spec.toString().startsWith("ChannelSpec(") shouldBe true
            correlation.toString() shouldBe "Correlation"
        }
    }

    context("a channel that cannot open") {
        test("fails without a codec, without the WebSockets plugin, credentials, a base or an allowed destination") {
            val secured = pets.change { security(listOf(SecurityUse("x", CredentialPlacement.Bearer))) }
            val png = ChannelSpec(
                sendSerializer = Message.serializer(),
                receiveDeserializer = Message.serializer(),
                settings = channelSettings { mediaType = MediaType.parse("image/png") },
            )
            val private = apiCall(operation = "chat", method = "GET") { destination("wss://10.0.0.1/chat") }

            outcomeOf(Exchanges(), channel = png) shouldBe
                ChannelOutcome.Failed(CallFailure.ParameterRefused("image/png"))
            outcomeOf(Exchanges()) shouldBe ChannelOutcome.Failed(CallFailure.ChannelsNotInstalled)
            outcomeOf(sockets(), call = secured) shouldBe ChannelOutcome.Failed(CallFailure.CredentialMissing)
            outcomeOf(sockets { baseUrl = "" }) shouldBe ChannelOutcome.Failed(CallFailure.BaseUrlMissing)
            outcomeOf(sockets(), call = private) shouldBe
                ChannelOutcome.Failed(CallFailure.DestinationRefused(DestinationRefusal.NotPublic))
        }

        test("fails when the engine cannot open it") {
            val exchanges = sockets()

            outcomeOf(exchanges).shouldBeInstanceOf<ChannelOutcome.Failed>()
            exchanges.urls.single() shouldBe "wss://api.example.com/v1/pets"
        }
    }
})
