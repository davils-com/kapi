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

package com.davils.kapi.spec.openapi.model

import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.openapi.model.parameter.ParameterPlacement
import com.davils.kapi.spec.openapi.model.parameter.ParameterStyle
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.schema.Bound
import com.davils.kapi.spec.openapi.model.schema.SchemaType
import com.davils.kapi.spec.openapi.model.security.ApiKeyPlacement
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val UNKNOWN: String = "unknown"

class KeywordsTest : FunSpec({
    context("a keyword") {
        test("names every method and finds it again") {
            Method.entries.forEach { method ->
                Method.fromKeyword(method.keyword) shouldBe method
            }
            Method.fromKeyword("GET") shouldBe null
        }

        test("names every placement of a parameter and finds it again") {
            ParameterPlacement.entries.forEach { placement ->
                ParameterPlacement.fromKeyword(placement.keyword) shouldBe placement
            }
            ParameterPlacement.fromKeyword(UNKNOWN) shouldBe null
        }

        test("names every style and finds it again") {
            ParameterStyle.entries.forEach { style ->
                ParameterStyle.fromKeyword(style.keyword) shouldBe style
            }
            ParameterStyle.fromKeyword(UNKNOWN) shouldBe null
        }

        test("names every placement of an api key and finds it again") {
            ApiKeyPlacement.entries.forEach { placement ->
                ApiKeyPlacement.fromKeyword(placement.keyword) shouldBe placement
            }
            ApiKeyPlacement.fromKeyword(UNKNOWN) shouldBe null
        }

        test("names every type of a schema and finds it again") {
            SchemaType.entries.forEach { type ->
                SchemaType.fromKeyword(type.keyword) shouldBe type
            }
            SchemaType.fromKeyword(UNKNOWN) shouldBe null
        }
    }

    context("a style") {
        test("has a default for every placement") {
            val path = ParameterStyle.defaultFor(ParameterPlacement.Path)
            val query = ParameterStyle.defaultFor(ParameterPlacement.Query)
            val header = ParameterStyle.defaultFor(ParameterPlacement.Header)
            val cookie = ParameterStyle.defaultFor(ParameterPlacement.Cookie)

            listOf(path, query, header, cookie) shouldBe listOf(
                ParameterStyle.Simple,
                ParameterStyle.Form,
                ParameterStyle.Simple,
                ParameterStyle.Form,
            )
        }

        test("is allowed only where the specification allows it") {
            ParameterStyle.Matrix.isAllowedIn(ParameterPlacement.Path) shouldBe true
            ParameterStyle.Matrix.isAllowedIn(ParameterPlacement.Query) shouldBe false
            ParameterStyle.DeepObject.isAllowedIn(ParameterPlacement.Query) shouldBe true
            ParameterStyle.Form.isAllowedIn(ParameterPlacement.Header) shouldBe false
            ParameterStyle.Form.isAllowedIn(ParameterPlacement.Cookie) shouldBe true
        }
    }

    context("a bound") {
        test("is inclusive unless it says otherwise") {
            val one = NumberValue.of(1L)

            Bound(one).isExclusive shouldBe false
            Bound(one, isExclusive = true).value shouldBe one
        }
    }
})
