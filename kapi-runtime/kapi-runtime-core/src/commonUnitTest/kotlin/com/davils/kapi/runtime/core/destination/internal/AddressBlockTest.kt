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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AddressBlockTest : FunSpec({
    test("refuses a block whose prefix is no address") {
        shouldThrow<IllegalArgumentException> { AddressBlock.of("x/8") }
    }

    test("contains only addresses of its family and prefix") {
        val block = AddressBlock.of("10.0.0.0/8")
        val inside = IpParser.parse("10.1.2.3")
        val outside = IpParser.parse("11.1.2.3")
        val other = IpParser.parse("::1")

        inside?.let(block::contains) shouldBe true
        outside?.let(block::contains) shouldBe false
        other?.let(block::contains) shouldBe false
    }
})
