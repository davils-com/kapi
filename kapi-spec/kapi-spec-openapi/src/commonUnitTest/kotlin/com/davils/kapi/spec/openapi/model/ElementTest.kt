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

package com.davils.kapi.spec.openapi.model

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.value.ExtensionName
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.info.ContactBuilder
import com.davils.kapi.spec.openapi.model.info.LicenseBuilder
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith

private val audited: ExtensionName = ExtensionName("x-audited")

private val here: Location = Location(DocumentUri.MEMORY, JsonPointer.of("info"))

class ElementTest : FunSpec({
    context("every element") {
        test("builds without a finding from the defaults of its builder") {
            elementBuilders.forEach { create ->
                val report = create().validate()

                report.violations.shouldBeEmpty()
            }
        }

        test("equals an element that was built alike and renders that it was built") {
            elementBuilders.forEach { create ->
                val element = create().build()
                val same = create().build()

                element shouldBe same
                element.hashCode() shouldBe same.hashCode()
                element.toString() shouldEndWith "(built)"
            }
        }
    }

    context("an element") {
        test("differs from an element of another kind, even when both are empty") {
            val contact = ContactBuilder().build()
            val license = LicenseBuilder().build()

            contact shouldNotBe license
            contact.equals("contact") shouldBe false
        }

        test("differs from an element of other content") {
            val builder = ContactBuilder()
            builder.name = "Davils"

            builder.build() shouldNotBe ContactBuilder().build()
            builder.build().name shouldBe "Davils"
        }

        test("carries its extensions and differs from an element without them") {
            val builder = ContactBuilder()
            builder.extension(audited, Value.Flag(true))
            val contact = builder.build()

            contact.extensions[audited] shouldBe Value.Flag(true)
            contact shouldNotBe ContactBuilder().build()
        }

        test("equals an element of the same content found elsewhere and renders where it was found") {
            val builder = ContactBuilder()
            builder.location = here
            val contact = builder.build()

            contact shouldBe ContactBuilder().build()
            contact.location shouldBe here
            contact.toString() shouldBe "Contact(memory:/document#/info)"
        }
    }
})
