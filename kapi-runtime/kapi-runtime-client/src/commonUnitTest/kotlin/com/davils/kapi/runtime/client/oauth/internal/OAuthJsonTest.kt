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

package com.davils.kapi.runtime.client.oauth.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

private const val DEPTH: Int = 17

private const val MOST: Long = 100

private const val NUMBER: Long = 7

private const val NUMBER_NAME: String = "number"

private const val TOO_LONG: Int = 65

private val list: JsonArray = buildJsonArray {
    add(JsonPrimitive("S256"))
    add(JsonPrimitive(1))
    add(buildJsonObject { })
}

private val json: JsonObject = buildJsonObject {
    put("text", JsonPrimitive("a"))
    put(NUMBER_NAME, JsonPrimitive(NUMBER))
    put("quoted", JsonPrimitive("8"))
    put("word", JsonPrimitive("x"))
    put(key = "list", element = list)
}

class OAuthJsonTest : FunSpec({
    context("the JSON of an authorization server") {
        test("is read only as an object of bounded depth, whatever brackets its strings hold") {
            OAuthJson.objectOf("[".repeat(DEPTH) + "]".repeat(DEPTH)) shouldBe null
            val nested = OAuthJson.objectOf("""{"a":"[[[[[[[[[[[[[[[[[[[[\"]]"}""")
            nested?.let { found -> OAuthJson.text(found, "a") } shouldBe "[[[[[[[[[[[[[[[[[[[[\"]]"
            OAuthJson.objectOf("[]") shouldBe null
            OAuthJson.objectOf("{") shouldBe null
        }

        test("hands out texts, numbers and lists of texts, and nothing of another type") {
            OAuthJson.text(json, "text") shouldBe "a"
            OAuthJson.text(json, NUMBER_NAME) shouldBe null
            OAuthJson.text(json, "list") shouldBe null
            OAuthJson.number(json, NUMBER_NAME) shouldBe NUMBER
            OAuthJson.number(json, "quoted") shouldBe NUMBER + 1
            OAuthJson.number(json, "word") shouldBe null
            OAuthJson.number(json, "missing") shouldBe null
            OAuthJson.texts(json, "list") shouldBe listOf("S256")
            OAuthJson.texts(json, "text") shouldBe emptyList()
        }

        test("names an error only as a short code of visible characters") {
            OAuthJson.errorOf("""{"error":"invalid_grant"}""") shouldBe "invalid_grant"
            OAuthJson.errorOf("""{"error":"${"x".repeat(TOO_LONG)}"}""") shouldBe null
            OAuthJson.errorOf("""{"error":""}""") shouldBe null
            OAuthJson.errorOf("""{"error":"a\"b"}""") shouldBe null
            OAuthJson.errorOf("""{"error":"a\\b"}""") shouldBe null
            OAuthJson.errorOf("""{"error":"a\u0001b"}""") shouldBe null
            OAuthJson.errorOf("""{"error":1}""") shouldBe null
            OAuthJson.errorOf("<html>") shouldBe null
        }

        test("bounds a number of seconds") {
            OAuthJson.seconds(value = null, most = MOST) shouldBe null
            OAuthJson.seconds(value = -1, most = MOST) shouldBe 0
            OAuthJson.seconds(value = MOST + 1, most = MOST) shouldBe MOST
        }
    }

    context("the answers of an authorization server") {
        test("are read when they hold what they must") {
            TokenResponse.of("""{"access_token":"t","token_type":"Bearer","expires_in":7}""")?.expiresIn shouldBe NUMBER
            TokenResponse.of("""{"token_type":"Bearer"}""") shouldBe null
            TokenResponse.of("""{"access_token":"t"}""") shouldBe null
            TokenResponse.of("x") shouldBe null
            DeviceResponse.of("""{"device_code":"d","user_code":"u"}""") shouldBe null
            DeviceResponse.of("x") shouldBe null
            MetadataResponse.of("""{"token_endpoint":"https://a"}""") shouldBe null
            MetadataResponse.of("x") shouldBe null
        }
    }
})
