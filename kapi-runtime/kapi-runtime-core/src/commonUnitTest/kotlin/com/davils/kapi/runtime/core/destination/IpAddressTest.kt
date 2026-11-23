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

package com.davils.kapi.runtime.core.destination

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val QUAD: String = "1.2.3.4"

private const val IPV4_BYTES: Int = 4

private const val IPV6_BYTES: Int = 16

private val QUAD_BYTES: ByteArray = byteArrayOf(1, 2, 3, IPV4_BYTES.toByte())

private val canonical: Map<String, String> = mapOf(
    "2001:0db8:0000:0000:0000:0000:0002:0001" to "2001:db8::2:1",
    "2001:db8:0:0:1:0:0:1" to "2001:db8::1:0:0:1",
    "2001:db8:0:1:1:1:1:1" to "2001:db8:0:1:1:1:1:1",
    "2001:DB8::AAAA" to "2001:db8::aaaa",
    "::" to "::",
    "::1" to "::1",
    "1::" to "1::",
    "0:0:0:0:0:ffff:192.0.2.1" to "::ffff:192.0.2.1",
    "::ffff:c000:0201" to "::ffff:192.0.2.1",
    "fe80:0:0:0:0:0:0:1" to "fe80::1",
    "1:0:0:2:0:0:0:3" to "1:0:0:2::3",
    "1:2:3:4:5:6:7::" to "1:2:3:4:5:6:7:0",
    "64:ff9b::192.0.2.33" to "64:ff9b::c000:221",
    "192.0.2.1" to "192.0.2.1",
    "0.0.0.0" to "0.0.0.0",
)

private val malformed: List<String> = listOf(
    "", "1.2.3", "1.2.3.4.5", "256.1.1.1", "01.1.1.1", "1.1.1.-1", "a.b.c.d", " 1.1.1.1", "1.1.1.1 ",
    ":", ":::", "1:2:3:4:5:6:7:8:9", "1:2:3:4:5:6:7", "12345::", "g::", "1::2::3", ":1:2:3:4:5:6:7", "1:2:3:4:5:6:7:",
    "::ffff:1.2.3", "::1.2.3.4:5", "fe80::1%eth0", "[::1]", "1:2:3:4:5:6:7:1.2.3.4", "1".repeat(50),
)

class IpAddressTest : FunSpec({
    context("reading") {
        canonical.forEach { (text, written) ->
            test("reads $text and writes it as $written") {
                IpAddress.parse(text).toString() shouldBe written
            }
        }

        malformed.forEach { text ->
            test("refuses '$text'") {
                IpAddress.parseOrNull(text).shouldBeNull()
            }
        }

        test("throws with the text escaped") {
            shouldThrow<IllegalArgumentException> { IpAddress.parse("x\n") }
        }
    }

    context("an address") {
        test("is equal by its bytes and has the size of its family") {
            IpAddress.parse("::ffff:1.2.3.4") shouldBe IpAddress.parse("0:0:0:0:0:ffff:102:304")
            val address = IpAddress.parse(QUAD)

            address shouldNotBe IpAddress.parse("::ffff:1.2.3.4")
            address.hashCode() shouldBe IpAddress.parse(QUAD).hashCode()
            address.isIpv4 shouldBe true
            IpAddress.parse("::1").isIpv4 shouldBe false
            address.toByteArray().size shouldBe IPV4_BYTES
            address.equals(QUAD) shouldBe false
        }

        test("is made of 4 or 16 bytes and nothing else") {
            IpAddress.fromByteArrayOrNull(QUAD_BYTES) shouldBe IpAddress.parse(QUAD)
            IpAddress.fromByteArrayOrNull(ByteArray(IPV6_BYTES)) shouldBe IpAddress.parse("::")
            IpAddress.fromByteArrayOrNull(ByteArray(IPV4_BYTES + 1)).shouldBeNull()
        }

        test("is public only in the global ranges") {
            listOf("93.184.216.34", "8.8.8.8", "2606:4700::1111", "::ffff:8.8.8.8", "2002:808:808::1").forEach { text ->
                IpAddress.parse(text).isPublic shouldBe true
            }
            listOf(
                "10.0.0.1",
                "100.127.255.255",
                "203.0.113.9",
                "::",
                "2001:db8::1",
                "5f00::1",
                "fec0::1",
            ).forEach { text ->
                IpAddress.parse(text).isPublic shouldBe false
            }
        }
    }
})
