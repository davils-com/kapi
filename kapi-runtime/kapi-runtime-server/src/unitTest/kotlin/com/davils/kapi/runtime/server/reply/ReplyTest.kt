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

package com.davils.kapi.runtime.server.reply

import com.davils.kapi.runtime.core.cache.CacheControl
import com.davils.kapi.runtime.core.cache.CacheDirective
import com.davils.kapi.runtime.core.cache.Vary
import com.davils.kapi.runtime.core.conditional.EntityTag
import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.header.ContentDisposition
import com.davils.kapi.runtime.core.header.LinkValue
import com.davils.kapi.runtime.core.header.RetryAfter
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.ratelimit.RateLimit
import com.davils.kapi.runtime.core.ratelimit.RateLimitPolicy
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.serveOne
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.io.bytestring.encodeToByteString

private const val ETAG: String = "ETag"

private const val OTHER_TAG: String = """"b""""

private const val OK: Int = 200

private const val NO_CONTENT: Int = 204

private const val NOT_MODIFIED: Int = 304

private const val TOO_MANY: Int = 429

private const val DELAY: Long = 30L

private const val QUOTA: Long = 100L

private const val REMAINING: Long = 7L

private const val EPOCH: Long = 784_111_777L

private val helpers: Reply = reply(TOO_MANY) {
    cacheControl(CacheControl.of(CacheDirective.noCache(), CacheDirective.maxAge(DELAY)))
    vary(Vary(listOf("Accept", "Accept-Language")))
    links(listOf(LinkValue("https://api.example.com/pets?page=2", listOf("rel" to "next"))))
    links(emptyList())
    retryAfter(RetryAfter.Delay(DELAY))
    rateLimits(listOf(RateLimit(name = "default", remaining = REMAINING)))
    rateLimits(emptyList())
    rateLimitPolicies(listOf(RateLimitPolicy(name = "default", quota = QUOTA)))
    rateLimitPolicies(emptyList())
    entityTag(EntityTag("v1"))
    lastModified(HttpDate(EPOCH))
    contentDisposition(ContentDisposition.attachment("pets.json"))
    header("X-Absent", null)
    json(Note.serializer(), Note("a"))
}

class ReplyTest : FunSpec({
    context("the helpers of a reply") {
        test("write every field from the values of the runtime core") {
            helpers.headers shouldBe listOf(
                "Cache-Control" to "no-cache, max-age=30",
                "Vary" to "accept, accept-language",
                "Link" to "<https://api.example.com/pets?page=2>; rel=\"next\"",
                "Retry-After" to "30",
                "RateLimit" to "\"default\";r=7",
                "RateLimit-Policy" to "\"default\";q=100",
                "ETag" to "\"v1\"",
                "Last-Modified" to "Sun, 06 Nov 1994 08:49:37 GMT",
                "Content-Disposition" to "attachment; filename=\"pets.json\"",
            )
            helpers.header("etag") shouldBe "\"v1\""
            helpers.header("x-absent") shouldBe null
        }

        test("are answered as they were written, with the body the codecs of the server write") {
            serveOne(handler = { helpers }) {
                val response = client.post(PROBE)

                response.status.value shouldBe TOO_MANY
                response.headers["RateLimit-Policy"] shouldBe "\"default\";q=100"
                response.bodyAsText() shouldBe """{"text":"a"}"""
            }
        }
    }

    context("a reply") {
        test("is a value equal to a reply with the same status, fields and body, and shows no content") {
            val first = reply(OK) { bytes(MediaType.OctetStream, "secret".encodeToByteString()) }
            val second = reply(OK) { bytes(MediaType.OctetStream, "secret".encodeToByteString()) }

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe reply(OK)
            first.toString() shouldBe "Reply(200, Bytes(application/octet-stream, 6 bytes))"
            reply(OK).toString() shouldBe "Reply(200, Empty)"
            reply(OK) { json(Note.serializer(), Note("a")) }.body.toString() shouldBe "Value(application/json)"
            reply(OK) { stream(MediaType.OctetStream, emptyFlow(), length = 2) }
                .body
                .toString() shouldBe "Stream(application/octet-stream, 2 bytes)"
            reply(OK) { events { } }.body.toString() shouldBe "Stream(text/event-stream; charset=utf-8, unknown bytes)"
        }

        test("keeps a field it has when a default is offered") {
            val tagged = reply(OK) { entityTag(EntityTag("a")) }

            val defaulted = reply(OK).withDefaultHeader(name = ETAG, value = OTHER_TAG)

            tagged.withDefaultHeader(name = ETAG, value = OTHER_TAG) shouldBe tagged
            defaulted.header(ETAG) shouldBe OTHER_TAG
        }

        test("without content is answered without a body, whatever it carries") {
            serveOne(handler = { reply(NO_CONTENT) { text("dropped") } }) {
                val response = client.post(PROBE)

                response.status.value shouldBe NO_CONTENT
                response.bodyAsBytes().size shouldBe 0
            }
            serveOne(handler = { reply(NOT_MODIFIED) }) {
                client.post(PROBE).status.value shouldBe NOT_MODIFIED
            }
        }

        test("of plain text and of bytes carries its media type") {
            serveOne(handler = { reply(OK) { text("hello") } }) {
                val response = client.post(PROBE)

                response.headers["Content-Type"] shouldBe "text/plain; charset=utf-8"
                response.bodyAsText() shouldBe "hello"
            }
        }

        test("written by a codec the server lacks fails without a word of why") {
            val unknown = MediaType.parse("application/x-unknown")

            serveOne(handler = { reply(OK) { body(unknown, Note.serializer(), Note("a")) } }) {
                client.post(PROBE).status.value shouldBe SERVER_ERROR
            }
        }
    }
})

private const val SERVER_ERROR: Int = 500
