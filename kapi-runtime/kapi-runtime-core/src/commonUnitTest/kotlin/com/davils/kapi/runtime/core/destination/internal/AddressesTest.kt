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

package com.davils.kapi.runtime.core.destination.internal

import com.davils.kapi.runtime.core.destination.IpAddress
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val ODD_SIZE: Int = 5

private fun isPublic(text: String): Boolean = Addresses.isPublic(IpAddress.parse(text).toByteArray())

class AddressesTest : FunSpec({
    test("counts only globally reachable public addresses as public") {
        listOf("8.8.8.8", "2001:4860:4860::8888", "1.1.1.1").forEach { text -> isPublic(text) shouldBe true }
    }

    test("refuses private, shared, loopback, documentation and reserved addresses") {
        listOf(
            "10.1.2.3",
            "100.64.0.1",
            "127.0.0.1",
            "169.254.169.254",
            "192.168.1.1",
            "198.51.100.7",
            "240.0.0.1",
            "::1",
            "fd00::1",
            "fe80::1",
            "2001:db8::1",
            "3fff::1",
        ).forEach { text -> isPublic(text) shouldBe false }
    }

    test("judges an IPv4 address embedded in a mapped, a NAT64 or a 6to4 address by itself") {
        isPublic("::ffff:127.0.0.1") shouldBe false
        isPublic("::ffff:8.8.8.8") shouldBe true
        isPublic("64:ff9b::7f00:1") shouldBe false
        isPublic("64:ff9b::808:808") shouldBe true
        isPublic("2002:7f00:1::") shouldBe false
        isPublic("2002:808:808::") shouldBe true
    }

    test("knows no address of another size") {
        Addresses.konnectOf(ByteArray(ODD_SIZE)).shouldBeNull()
        Addresses.isPublic(ByteArray(ODD_SIZE)) shouldBe false
    }
})
