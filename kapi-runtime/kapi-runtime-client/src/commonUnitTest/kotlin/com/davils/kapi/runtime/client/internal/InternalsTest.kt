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

package com.davils.kapi.runtime.client.internal

import com.davils.kapi.runtime.client.ClientEvent
import com.davils.kapi.runtime.client.call.Body
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.Upgrade
import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.destination.HostResolution
import com.davils.kapi.runtime.core.destination.IpAddress
import com.davils.kapi.runtime.core.destination.destinationPolicy
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.response.headersOf
import com.davils.konfetti.core.eventBus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.Url
import kotlinx.coroutines.coroutineScope
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val BASE: String = "https://a.example.com/x/y?q=1#f"

private const val TARGET: String = "https://b.example.com/v"

private const val SEE_OTHER: Int = 303

private const val MOVED: Int = 301

private const val TEMPORARY: Int = 307

private const val OK: Int = 200

private const val NOT_FOUND: Int = 404

private const val UNAVAILABLE: Int = 503

private const val HTTPS_PORT: Int = 8_443

private const val TRACE: String = "X-Trace"

private fun outgoing(method: String, headers: List<ParameterEntry> = emptyList(), upgrade: Upgrade? = null) = Outgoing(
    method = method,
    url = BASE,
    headers = headers,
    cookies = listOf(ParameterEntry(name = "session", value = "s1")),
    accept = emptyList(),
    body = Body(MediaType.Json, "{}".encodeToByteString()),
    upload = null,
    upgrade = upgrade,
    binding = null,
)

private fun entries(vararg names: String): List<ParameterEntry> = names.map { name ->
    ParameterEntry(
        name = name,
        value = "v",
    )
}

class InternalsTest : FunSpec({
    context("the text of a field") {
        test("is a token of RFC 9110 for a name, and visible characters, spaces and tabs for a value") {
            FieldText.isToken("") shouldBe false
            FieldText.isToken("a b") shouldBe false
            FieldText.isToken("Abz-019!#$%&'*+.^_`|~") shouldBe true
            FieldText.isToken("é") shouldBe false
            FieldText.isValue("a\tb ~") shouldBe true
            FieldText.isValue("a\nb") shouldBe false
            FieldText.isValue("é") shouldBe false
            FieldText.isCookieValue("abc") shouldBe true
            listOf(
                "a;b",
                "a b",
                "a\"b",
                "a\\b",
                "a,b",
                "é",
            ).forEach { value -> FieldText.isCookieValue(value) shouldBe false }
        }
    }

    context("the texts of the settings") {
        test("are lower case words and plain or protected URLs") {
            listOf("a", "a1", "a.b-c_d").forEach { name -> Texts.isLowerWords(name) shouldBe true }
            listOf("", "1a", "a.", "a..b", "A", "a b").forEach { name -> Texts.isLowerWords(name) shouldBe false }
            Texts.isPlainUrl("https://a.example.com/x") shouldBe true
            listOf(
                "ftp://a.example.com",
                "https://a.example.com?x",
                "https://a.example.com#x",
                "https://user@a.example.com",
                "https://a\\b.example.com",
                "not a url",
                "https://",
            ).forEach { url -> Texts.isPlainUrl(url) shouldBe false }
            listOf(
                "https://a.example.com",
                "http://localhost:1/x",
                "http://[::1]:9/x",
                "http://127.0.0.1",
            ).forEach { url ->
                Texts.isProtectedUrl(url) shouldBe true
            }
            listOf(
                "http://a.example.com",
                "ftp://localhost",
            ).forEach { url -> Texts.isProtectedUrl(url) shouldBe false }
        }
    }

    context("a reference") {
        test("is resolved against its base by RFC 3986, without a fragment") {
            References.resolve(base = BASE, reference = "") shouldBe "https://a.example.com/x/y?q=1"
            References.resolve(base = BASE, reference = "#g") shouldBe "https://a.example.com/x/y?q=1"
            References.resolve(base = BASE, reference = "?p=2") shouldBe "https://a.example.com/x/y?p=2"
            References.resolve(base = BASE, reference = "/z") shouldBe "https://a.example.com/z"
            References.resolve(base = BASE, reference = "w") shouldBe "https://a.example.com/x/w"
            References.resolve(base = BASE, reference = "$TARGET#g") shouldBe TARGET
        }
    }

    context("a destination") {
        test("is the scheme, the host in brackets for IPv6 and the port of a URL") {
            val six = Destinations.destinationOf(Url("https://[::1]:$HTTPS_PORT/x"))
            val plain = Destinations.destinationOf(Url("https://a.example.com/"))

            six.host shouldBe "[::1]"
            six.port shouldBe HTTPS_PORT
            plain.port shouldBe null
        }

        test("is checked once more at the addresses the policy resolved") {
            val public = destinationPolicy {
                resolver = {
                    HostResolution.Resolved(
                        listOf(IpAddress.parse("93.184.216.34")),
                    )
                }
            }
            val unresolved = destinationPolicy { resolver = { HostResolution.Failed("down") } }

            Destinations(public).refusal(TARGET) shouldBe null
            Destinations(
                unresolved,
            ).refusal(TARGET) shouldBe CallFailure.DestinationRefused(DestinationRefusal.Unresolved)
        }
    }

    context("a request that is redirected") {
        test("keeps HEAD with 303, keeps other methods but POST with 301, and drops content fields with GET") {
            val head = outgoing("HEAD").redirected(status = SEE_OTHER, target = TARGET, isSameOrigin = true)
            val put = outgoing("PUT").redirected(status = MOVED, target = TARGET, isSameOrigin = true)
            val post = outgoing("POST", entries("Content-Type", TRACE)).redirected(
                status = MOVED,
                target = TARGET,
                isSameOrigin = true,
            )

            head?.method shouldBe "HEAD"
            put?.method shouldBe "PUT"
            put?.body shouldBe outgoing("PUT").body
            post?.method shouldBe "GET"
            post?.run { headers.map(ParameterEntry::name) } shouldBe listOf(TRACE)
        }

        test(
            "leaves its credentials, cookies and proofs behind on another origin, and is not replayed when it switches",
        ) {
            val crossed = outgoing("GET", entries("Authorization", "Cookie", "DPoP", TRACE)).redirected(
                status = TEMPORARY,
                target = TARGET,
                isSameOrigin = false,
            )
            val switching = outgoing("GET", upgrade = Upgrade("tcp")).redirected(
                status = TEMPORARY,
                target = TARGET,
                isSameOrigin = true,
            )

            crossed?.run { headers.map(ParameterEntry::name) } shouldBe listOf(TRACE)
            crossed?.cookies shouldBe emptyList()
            switching shouldBe null
            outgoing("GET").isReplayable shouldBe true
        }
    }

    context("an event sink") {
        test("offers events only to a bus whose topic has consumers") {
            var built = 0
            EventSink(null, ClientEvent.TOPIC).emit {
                built += 1
                ClientEvent.Retrying(api = "a", operation = "o", attempt = 2)
            }
            coroutineScope {
                val bus = eventBus(this)
                val sink = EventSink(bus, ClientEvent.TOPIC)
                sink.emit {
                    built += 1
                    ClientEvent.Retrying(api = "a", operation = "o", attempt = 2)
                }
                (ClientEvent.TOPIC in bus) shouldBe true
                EventSink(bus, ClientEvent.TOPIC).emit { ClientEvent.Retrying(api = "a", operation = "o", attempt = 2) }
                bus.close()
                EventSink(bus, ClientEvent.TOPIC).emit { ClientEvent.Retrying(api = "a", operation = "o", attempt = 2) }
            }

            built shouldBe 0
        }
    }

    context("the outcome of a call") {
        test("is told by its status, or failed without an answer") {
            val answers = listOf(OK, NOT_FOUND, UNAVAILABLE).map { status ->
                CallOutcome.of(
                    Exchange.Answered(status = status, headers = headersOf(emptyMap()), content = ByteString()),
                )
            }

            answers shouldBe listOf(CallOutcome.Success, CallOutcome.ClientError, CallOutcome.ServerError)
            CallOutcome.of(Exchange.Failed(CallFailure.TimedOut)) shouldBe CallOutcome.Failed
        }
    }
})
