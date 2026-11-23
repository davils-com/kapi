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

package com.davils.kapi.runtime.core.problem

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

private const val BELOW: Int = 99

private const val ABOVE: Int = 600

private const val NOT_FOUND: Int = 404

private const val PATH: String = "/a"

private const val CODE: String = "c"

private const val MESSAGE: String = "m"

@OptIn(ExperimentalSerializationApi::class)
private val plainEncoder: Encoder = object : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
}

@OptIn(ExperimentalSerializationApi::class)
private val plainDecoder: Decoder = object : AbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = CompositeDecoder.DECODE_DONE
}

class ProblemFormatsTest : FunSpec({
    context("the serializer") {
        test("refuses a format that is not JSON") {
            shouldThrow<SerializationException> { ProblemSerializer.serialize(plainEncoder, Problem()) }
            shouldThrow<SerializationException> { ProblemSerializer.deserialize(plainDecoder) }
        }

        test("ignores a status below 100") {
            Json.decodeFromString(ProblemSerializer, """{"status":99}""").status.shouldBeNull()
        }
    }

    context("a problem") {
        test("has a status between 100 and 599") {
            shouldThrow<IllegalArgumentException> { Problem(status = BELOW) }
            shouldThrow<IllegalArgumentException> { Problem(status = ABOVE) }
        }

        test("shows its status and differs from what is no problem") {
            Problem(status = NOT_FOUND).toString() shouldBe "Problem(type=about:blank, status=404, violations=0, " +
                "extensions=[])"
            Problem().equals(NOT_FOUND) shouldBe false
        }
    }

    context("a violation") {
        test("differs in its path, its code and its message") {
            val violation = ProblemViolation(path = PATH, code = CODE, message = MESSAGE)

            violation shouldNotBe ProblemViolation(path = "/b", code = CODE, message = MESSAGE)
            violation shouldNotBe ProblemViolation(path = PATH, code = "d", message = MESSAGE)
            violation shouldNotBe ProblemViolation(path = PATH, code = CODE, message = "n")
            violation.equals(PATH) shouldBe false
        }
    }
})
