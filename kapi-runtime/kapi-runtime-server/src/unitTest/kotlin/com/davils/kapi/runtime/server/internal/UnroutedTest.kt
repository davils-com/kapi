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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.server.Kapi
import com.davils.kapi.runtime.server.fixture.probe
import com.davils.kapi.runtime.server.request.ParameterType
import com.davils.kapi.runtime.server.request.Place
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.application.plugin
import io.ktor.server.testing.runTestApplication
import kotlinx.serialization.builtins.serializer

private const val BAD_REQUEST: Int = 400

private val limit: ParameterFormat = ParameterFormat("limit", ParameterStyle.Simple, isExploded = false)

private val number: ParameterType<Int> = ParameterType(Int.serializer(), isQuoted = false)

class UnroutedTest : FunSpec({
    context("a call that did not come through routing") {
        test("has no parameters of the path, so a query field of the same name stands in for none of them") {
            val operation = probe(path = "/items/{limit}", method = "GET") { input ->
                input.value(place = Place.Path, format = limit, type = number, isRequired = true)
            }

            runTestApplication {
                application {
                    install(Kapi)
                    val exchange = Exchange(plugin(Kapi), operation)
                    intercept(ApplicationCallPipeline.Call) { exchange.serve(context) }
                }

                client.get("/items/5?limit=9").status.value shouldBe BAD_REQUEST
            }
        }
    }
})
