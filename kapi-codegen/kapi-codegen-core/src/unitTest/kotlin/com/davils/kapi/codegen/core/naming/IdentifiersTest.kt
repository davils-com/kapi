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


package com.davils.kapi.codegen.core.naming

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val typeNames: Map<String, String> = mapOf(
    "pet" to "Pet",
    "pet-store" to "PetStore",
    "pet_store.item" to "PetStoreItem",
    "petStoreItem" to "PetStoreItem",
    "HTTPServerError" to "HttpServerError",
    "v2Pet" to "V2Pet",
    "2xx" to "Type2xx",
    "---" to "Value",
    "" to "Value",
)

private val memberNames: Map<String, String> = mapOf(
    "pet" to "pet",
    "Pet-Store" to "petStore",
    "created_at" to "createdAt",
    "X-Request-ID" to "xRequestId",
    "2fa" to "n2fa",
    "$" to "value",
)

private val constantNames: Map<String, String> = mapOf(
    "available" to "AVAILABLE",
    "in-stock" to "IN_STOCK",
    "soldOut" to "SOLD_OUT",
    "1st" to "N1ST",
    "+" to "EMPTY",
)

class IdentifiersTest : FunSpec({
    context("names") {
        typeNames.forEach { (raw, name) ->
            test("names the type '$raw' $name") {
                Identifiers.type(raw) shouldBe name
            }
        }

        memberNames.forEach { (raw, name) ->
            test("names the member '$raw' $name") {
                Identifiers.member(raw) shouldBe name
            }
        }

        constantNames.forEach { (raw, name) ->
            test("names the constant '$raw' $name") {
                Identifiers.constant(raw) shouldBe name
            }
        }
    }

    context("checks") {
        test("know an identifier from a keyword, a number and a lone underscore") {
            Identifiers.isIdentifier("pet_1") shouldBe true
            Identifiers.isIdentifier("_pet") shouldBe true
            Identifiers.isIdentifier("1pet") shouldBe false
            Identifiers.isIdentifier("pet-store") shouldBe false
            Identifiers.isIdentifier("__") shouldBe false
            Identifiers.isIdentifier("when") shouldBe false
            Identifiers.isIdentifier("") shouldBe false
        }

        test("know a qualified name") {
            Identifiers.isQualified("kotlin.uuid.Uuid") shouldBe true
            Identifiers.isQualified("Money") shouldBe true
            Identifiers.isQualified("kotlin..Uuid") shouldBe false
            Identifiers.isQualified("kotlin.fun.Uuid") shouldBe false
        }

        test("know a type name and a package") {
            Identifiers.isTypeName("Petstore2") shouldBe true
            Identifiers.isTypeName("pet") shouldBe false
            Identifiers.isTypeName("Pet_Store") shouldBe false
            Identifiers.isPackage("com.example.api_v2") shouldBe true
            Identifiers.isPackage("com.example.") shouldBe false
            Identifiers.isPackage("com.object") shouldBe false
        }
    }
})
