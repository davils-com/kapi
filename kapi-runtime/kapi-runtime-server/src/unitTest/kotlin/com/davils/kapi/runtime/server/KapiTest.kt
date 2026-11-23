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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.core.codec.codecs
import com.davils.kapi.runtime.server.fixture.PetsRoutes
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.fixture.serve
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.security.Authorizer
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.plugin
import io.ktor.server.routing.routing
import io.ktor.server.testing.runTestApplication

private const val CREATED: Int = 201

private const val INTERNAL: Int = 500

class KapiTest : FunSpec({
    context("a generated route class mounted into the routing of an application") {
        test("serves its operations on their methods, arbitrary methods and QUERY included, and path templates") {
            serve(routes = { mount(PetsRoutes()) }) {
                val query = client.request("/pets") { method = HttpMethod("QUERY") }
                val purge = client.request("/pets/rex") { method = HttpMethod("PURGE") }

                query.status.value shouldBe CREATED
                query.bodyAsText() shouldBe "query"
                purge.bodyAsText() shouldBe "rex"
            }
        }

        test("answers a failing handler with a problem that says nothing of the failure") {
            serve(routes = { mount(PetsRoutes()) }) {
                val response = client.request("/pets/fail")

                response.status.value shouldBe INTERNAL
                problemOf(response).code shouldBe ServerCodes.INTERNAL
                response.bodyAsText() shouldNotContain "secret"
            }
        }

        test("has no channels unless it declares them") {
            PetsRoutes().channels.shouldBeEmpty()
        }
    }

    context("the plugin") {
        test("is the server of the application with the settings it was installed with") {
            var server: ApiServer? = null
            runTestApplication {
                application {
                    install(Kapi) {
                        codecs = codecs { maxDepth = DEPTH }
                        realm = "pets"
                        sensitiveHeaders = setOf("X-Session")
                        problems { typeBase = "https://example.com/problems/" }
                    }
                    server = plugin(Kapi)
                }
                startApplication()
            }
            val installed = checkNotNull(server)
            val settings = installed.settings

            settings.realm shouldBe "pets"
            settings.codecs.maxDepth shouldBe DEPTH
            settings.sensitiveHeaders shouldBe setOf("X-Session")
            installed.toString() shouldContain "realm=pets"
            settings.problems.toString() shouldContain "problems"
        }

        test("refuses a realm that cannot be quoted and a scheme name that is no token") {
            val failure = shouldThrow<ValidationException> {
                val builder = ServerSettingsBuilder()
                builder.realm = "a\"b"
                builder.authorizer("not a token", Authorizer.DenyAll)
                builder.build()
            }

            failure.report.violations.map { violation -> violation.code } shouldBe listOf(
                ServerViolationCodes.REALM_INVALID,
                ServerViolationCodes.SCHEME_INVALID,
            )
        }

        test("refuses to mount when it is not installed") {
            shouldThrow<IllegalStateException> {
                runTestApplication {
                    application { notInstalled() }
                    startApplication()
                }
            }
        }

        test("names the version of its contract") {
            ServerContract.VERSION shouldBe 1
        }
    }

    context("an operation") {
        test("refuses a method, a path or a name that no route can carry") {
            shouldThrow<IllegalArgumentException> {
                Operation(
                    name = "a",
                    method = "G ET",
                    path = "/a",
                ) { reply(CREATED) }
            }
            shouldThrow<IllegalArgumentException> {
                Operation(
                    name = "a",
                    method = "GET",
                    path = "a",
                ) { reply(CREATED) }
            }
            shouldThrow<IllegalArgumentException> {
                Operation(
                    name = " ",
                    method = "GET",
                    path = "/a",
                ) { reply(CREATED) }
            }
            listOf("/a/{b", "/a/{*}", "/a/{b?}", "/a/{{b}}", "/a?b", "/a/ b", "/a/*").forEach { path ->
                shouldThrow<IllegalArgumentException> {
                    Operation(
                        name = "a",
                        method = "GET",
                        path = path,
                    ) { reply(CREATED) }
                }
            }
        }

        test("shows its name, its method and its path") {
            val operation = Operation(name = "getPet", method = "GET", path = "/pets/{id}.json") { reply(CREATED) }

            operation.toString() shouldBe "Operation(getPet, GET /pets/{id}.json)"
        }
    }

    context("a server with a base of problem types") {
        test("answers problems whose type is the base followed by their code") {
            serve(
                settings = { problems { typeBase = "https://example.com/problems/" } },
                routes = { mount(PetsRoutes()) },
            ) {
                problemOf(
                    client.request("/pets/fail"),
                ).type shouldBe "https://example.com/problems/kapi.server.internal"
            }
        }
    }
})

private const val DEPTH: Int = 8

private fun Application.notInstalled() {
    routing { mount(PetsRoutes()) }
}
