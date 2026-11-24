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

package com.davils.kapi.codegen.core.asyncapi

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.codegen.core.asyncapi.emit.Kdocs
import com.davils.kapi.codegen.core.asyncapi.plan.DirectionPlan
import com.davils.kapi.codegen.core.asyncapi.plan.MessagePlan
import com.davils.kapi.codegen.core.asyncapi.plan.PayloadSource
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private fun message(tag: String?): MessagePlan = MessagePlan(
    key = "said",
    member = "Said",
    payload = PayloadSource.Anything,
    correlation = null,
    summary = null,
    location = Location(DocumentUri.MEMORY, JsonPointer.ROOT),
    tag = tag,
)

class KdocsTest : FunSpec({
    context("the KDoc of a message") {
        test("names the property and the value that tell it apart") {
            val tagged = message("said")
            val direction = DirectionPlan(messages = listOf(tagged), operations = emptyList(), discriminator = "kind")

            Kdocs.message(direction, tagged) shouldContain "Its property `kind` is `said`."
        }

        test("names no property for a message without a tag") {
            val plain = message(null)
            val direction = DirectionPlan(messages = listOf(plain), operations = emptyList())

            Kdocs.message(direction, plain) shouldNotContain "Its property"
        }

        test("still names the value of a tag whose direction has no discriminator") {
            val tagged = message("said")
            val direction = DirectionPlan(messages = listOf(tagged), operations = emptyList())

            Kdocs.message(direction, tagged) shouldContain "Its property `` is `said`."
        }
    }
})
