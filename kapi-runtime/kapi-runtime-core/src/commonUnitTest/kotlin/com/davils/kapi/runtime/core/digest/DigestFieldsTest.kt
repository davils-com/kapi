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

package com.davils.kapi.runtime.core.digest

import com.davils.kapi.runtime.core.fieldLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.io.bytestring.ByteString
import kotlin.io.encoding.Base64

private const val SHA_256: String = "sha-256"

private const val SHA_512: String = "sha-512"

private const val HELLO_WORLD: String = "X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE="

private const val TOP_WEIGHT: Int = 10

private const val LOW_WEIGHT: Int = 3

private val helloWorld: ByteString = ByteString(Base64.decode(HELLO_WORLD))

private val other: ByteString = ByteString(byteArrayOf(1, 2, 3))

class DigestFieldsTest : FunSpec({
    context("Content-Digest and Repr-Digest") {
        test("reads the example of RFC 9530 §3 and writes it back") {
            val text = "$SHA_256=:$HELLO_WORLD:"

            DigestFields.parseDigests(text) shouldBe listOf(SHA_256 to helloWorld)
            DigestFields.formatDigests(listOf(SHA_256 to helloWorld)) shouldBe text
        }

        test("keeps unknown algorithms and ignores members that are no byte sequence") {
            val digests = DigestFields.parseDigests("sha-256=:AQID:, id-sha-1=:AQID:, md5=\"x\", crc32c=7")

            digests shouldBe listOf(SHA_256 to other, "id-sha-1" to other)
        }

        test("refuses what is no dictionary and what exceeds the limits") {
            DigestFields.parseDigests("sha-256=:AQID").shouldBeNull()
            DigestFields.parseDigests("SHA-256=:AQID:").shouldBeNull()
            DigestFields.parseDigests("a=:AQID:, b=:AQID:", fieldLimits { maxMembers = 1 }).shouldBeNull()
            shouldThrow<IllegalArgumentException> { DigestFields.formatDigests(listOf("SHA-256" to other)) }
        }

        test("chooses the strongest active algorithm") {
            val digests = listOf("md5" to other, SHA_256 to helloWorld, SHA_512 to other, "x" to other)

            DigestFields.strongest(digests) shouldBe (DigestAlgorithm.Sha512 to other)
            DigestFields.strongest(listOf("md5" to other, "sha" to other)).shouldBeNull()
        }
    }

    context("Want-Content-Digest and Want-Repr-Digest") {
        test("reads the example of RFC 9530 §4 and writes it back") {
            val text = "$SHA_512=3, $SHA_256=10, unixsum=0"
            val preferences = DigestFields.parsePreferences(text).shouldNotBeNull()

            preferences shouldBe listOf(SHA_512 to LOW_WEIGHT, SHA_256 to TOP_WEIGHT, "unixsum" to 0)
            DigestFields.formatPreferences(preferences) shouldBe text
            DigestFields.choose(preferences, listOf(DigestAlgorithm.Sha512, DigestAlgorithm.Sha256)) shouldBe
                DigestAlgorithm.Sha256
            DigestFields.choose(preferences, listOf(DigestAlgorithm.UnixSum)).shouldBeNull()
        }

        test("ignores weights outside 0 to 10 and refuses to write them") {
            DigestFields.parsePreferences("sha-256=11, sha-512=-1, md5=\"1\", sha=1") shouldBe listOf("sha" to 1)
            DigestFields.parsePreferences("sha-256=").shouldBeNull()
            shouldThrow<IllegalArgumentException> { DigestFields.formatPreferences(listOf(SHA_256 to TOP_WEIGHT + 1)) }
        }
    }

    context("the registry") {
        test("marks only SHA-256 and SHA-512 as active") {
            DigestAlgorithm.entries.filter(DigestAlgorithm::isActive) shouldBe
                listOf(DigestAlgorithm.Sha512, DigestAlgorithm.Sha256)
            DigestAlgorithm.of("SHA-256") shouldBe DigestAlgorithm.Sha256
            DigestAlgorithm.of("blake3").shouldBeNull()
        }
    }
})
