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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.multipart.MultipartFailure
import com.davils.kapi.runtime.core.header.FieldSyntax
import com.davils.kapi.runtime.core.response.HttpStatus
import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.InnerList
import com.davils.kapi.runtime.core.structured.Item
import com.davils.kapi.runtime.core.structured.Parameters
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.requestOf
import com.davils.kapi.runtime.server.operation.CredentialSource
import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.security.Authorization
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.routing.get
import io.ktor.server.testing.runTestApplication
import kotlinx.io.bytestring.encodeToByteString

private const val OAUTH: String = "oauth"

private const val BEARER_REALM: String = """Bearer realm="api""""

private const val OK: Int = 200

private const val LONG: Int = 2_049

private const val HUGE: Int = 65_537

class InternalsTest : FunSpec({
    context("the grammar of path templates and methods") {
        test("accepts tokens and templates and refuses what a route cannot carry") {
            PathTemplates.isMethod("QUERY") shouldBe true
            PathTemplates.isMethod("Query") shouldBe true
            PathTemplates.isMethod("Q UERY") shouldBe false
            PathTemplates.isMethod("") shouldBe false
            FieldSyntax.isToken("a b") shouldBe false
            PathTemplates.isTemplate("/a/{b}-{c}.json") shouldBe true
            PathTemplates.isTemplate("/" + "a".repeat(LONG)) shouldBe false
            PathTemplates.isTemplate("/a#b") shouldBe false
            PathTemplates.isTemplate("/a/}b{") shouldBe false
            PathTemplates.isTemplate("/a/{}") shouldBe false
            PathTemplates.isTemplate("/a/\u0001") shouldBe false
            PathTemplates.names("/a/{b}/{c}.{d}") shouldBe listOf("b", "c", "d")
            PathTemplates.isTemplate("/A9-_/{Z0}") shouldBe true
            PathTemplates.isTemplate("/a/{b}{c}") shouldBe true
            PathTemplates.isTemplate("/a/}{") shouldBe false
            PathTemplates.isTemplate("/a/{{b}") shouldBe false
            PathTemplates.isTemplate("/a/{b}}") shouldBe false
            PathTemplates.isTemplate("/a/{b c}") shouldBe false
            PathTemplates.isTemplate("/a/{b?}") shouldBe false
            PathTemplates.isTemplate("/a?") shouldBe false
            PathTemplates.isTemplate("/a/{é}") shouldBe false
            FieldSyntax.isToken("a~|`^") shouldBe true
        }
    }

    context("the checks of header fields") {
        test("allow a tab and refuse control characters, deletes, line breaks and overlong values") {
            HeaderChecks.isValue("a\tb") shouldBe true
            HeaderChecks.isValue("a\u007Fb") shouldBe false
            HeaderChecks.isValue("x".repeat(HUGE)) shouldBe false
            HeaderChecks.isLine("a\u0000") shouldBe false
            HeaderChecks.isLine("a\rb") shouldBe false
            HeaderChecks.isLine("a\nb") shouldBe false
            HeaderChecks.isLine("ab") shouldBe true
        }
    }

    context("the refusal of a multipart body") {
        test("is 413 for every broken limit and 400 for every broken grammar") {
            MultipartFailure.entries.associateWith(MultipartRefusals::statusOf) shouldBe mapOf(
                MultipartFailure.TooManyParts to HttpStatus.CONTENT_TOO_LARGE,
                MultipartFailure.HeadersTooLong to HttpStatus.CONTENT_TOO_LARGE,
                MultipartFailure.TooManyHeaders to HttpStatus.CONTENT_TOO_LARGE,
                MultipartFailure.MalformedHeader to HttpStatus.BAD_REQUEST,
                MultipartFailure.PartTooLarge to HttpStatus.CONTENT_TOO_LARGE,
                MultipartFailure.BodyTooLarge to HttpStatus.CONTENT_TOO_LARGE,
                MultipartFailure.PreambleTooLong to HttpStatus.CONTENT_TOO_LARGE,
                MultipartFailure.MalformedDelimiter to HttpStatus.BAD_REQUEST,
                MultipartFailure.Truncated to HttpStatus.BAD_REQUEST,
            )
        }
    }

    context("a content coding") {
        test("is read without case and blanks, and unknown or absent codings are told apart") {
            ContentCoding.of("") shouldBe ContentCoding.Identity
            ContentCoding.of("gzip,gzip") shouldBe ContentCoding.Unsupported
            ContentCoding.of(" GZIP ") shouldBe ContentCoding.Gzip
            ContentCoding.of("x-gzip") shouldBe ContentCoding.Gzip
            ContentCoding.of("br") shouldBe ContentCoding.Unsupported
        }
    }

    context("the challenges of a refusal") {
        val challenges = Challenges("api")
        val bearer = SchemeUse(OAUTH, CredentialSource.Bearer)

        test("carry an error code and a description only when they are safe to quote") {
            challenges.unauthorized(listOf(bearer), mapOf(OAUTH to Authorization.Denied("bad code"))) shouldBe
                listOf(BEARER_REALM)
            challenges.unauthorized(listOf(bearer), mapOf(OAUTH to Authorization.Denied("invalid_request"))) shouldBe
                listOf("""Bearer realm="api", error="invalid_request"""")
            challenges.unauthorized(
                listOf(bearer),
                mapOf(
                    OAUTH to Authorization.Denied(
                        error = "invalid_token",
                        description = """a "quoted"\ text""" + "\n",
                    ),
                ),
            ) shouldBe listOf("""Bearer realm="api", error="invalid_token", error_description="a quoted text"""")
        }

        test("of a forbidden bearer carry the scopes that are tokens, and nothing for another scheme") {
            challenges.forbidden(bearer, Authorization.Forbidden(setOf("a", "b c"))) shouldBe
                listOf("""Bearer realm="api", error="insufficient_scope", scope="a"""")
            challenges.forbidden(bearer, Authorization.Forbidden()) shouldBe
                listOf("""Bearer realm="api", error="insufficient_scope"""")
            challenges.forbidden(SchemeUse("k", CredentialSource.Header("k")), Authorization.Forbidden()) shouldBe
                emptyList()
        }
    }

    context("the base of a signature") {
        test("reads the path, the query and the fields of a request as RFC 9421 derives them") {
            val request = requestOf(headers = mapOf("x-a" to listOf(" 1 ", "2")), path = "", rawQuery = null)
            val covered = InnerList(
                listOf(
                    "@path",
                    "@query",
                    "@request-target",
                    "@target-uri",
                    "x-a",
                ).map { name -> Item(BareItem.Text(name)) },
            )

            SignatureBase.of(request, covered) shouldBe listOf(
                """"@path": /""",
                """"@query": ?""",
                """"@request-target": """,
                """"@target-uri": https://example.com""",
                """"x-a": 1, 2""",
                """"@signature-params": ("@path" "@query" "@request-target" "@target-uri" "x-a")""",
            ).joinToString("\n")
        }

        test("refuses a component that is no string, has parameters or is derived but unknown") {
            val request = requestOf()

            SignatureBase.of(request, InnerList(listOf(Item(BareItem.Integer(1))))) shouldBe null
            SignatureBase.of(request, InnerList(listOf(Item(BareItem.Text("@status"))))) shouldBe null
            SignatureBase.of(
                request,
                InnerList(listOf(Item(BareItem.Text("x-a"), Parameters.of("sf" to BareItem.Flag(true))))),
            ) shouldBe null
        }
    }

    context("the writer of replies") {
        test("writes a value with the codec of its media type") {
            runTestApplication {
                routing {
                    get(
                        "/",
                    ) { ReplyWriter(Codecs.Default).write(call, reply(OK) { json(Note.serializer(), Note("a")) }) }
                }

                client.get("/").bodyAsText() shouldBe """{"text":"a"}"""
            }
        }

        test("leaves a reply without a value as it is") {
            val plain = reply(OK) { text("a") }

            ReplyWriter(Codecs.Default).encoded(plain) shouldBe plain
            val written = ReplyWriter(Codecs.Default).encoded(reply(OK) { json(Note.serializer(), Note("a")) })

            written.body.toString() shouldBe
                "Bytes(application/json, ${"""{"text":"a"}""".encodeToByteString().size} bytes)"
        }
    }

    context("the challenge of a denial") {
        test("drops a description without an error code and an error code that is empty") {
            val challenges = Challenges("api")
            val bearer = SchemeUse(OAUTH, CredentialSource.Bearer)

            challenges.unauthorized(listOf(bearer), mapOf(OAUTH to Authorization.Denied(null, "d"))) shouldBe
                listOf(BEARER_REALM)
            challenges.unauthorized(
                listOf(bearer),
                mapOf(OAUTH to Authorization.Denied(error = "", description = "d")),
            ) shouldBe
                listOf(BEARER_REALM)
            challenges.unauthorized(
                listOf(bearer),
                mapOf(OAUTH to Authorization.Denied(error = "a!#]~", description = "! #]~é")),
            ) shouldBe
                listOf("""Bearer realm="api", error="a!#]~", error_description="! #]~"""")
        }
    }
})
