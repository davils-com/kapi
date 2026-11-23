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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.reading32
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val NAMED: String = "encoding: {file: {contentType: image/png}}"

private const val PREFIX: String = "prefixEncoding: [{contentType: text/plain}]"

private const val ITEM: String = "itemEncoding: {contentType: text/plain}"

private fun mediaTypes(vararg media: String): List<String> =
    listOf("components:", "  mediaTypes:") + media.mapIndexed { index, fields -> "    M$index: {$fields}" }

class EncodingRulesTest : FunSpec({
    context("the encodings of a media type") {
        test("are given by name or by position") {
            val reading = reading32(mediaTypes(NAMED, PREFIX, ITEM, "$PREFIX, $ITEM"))

            reading.diagnostics.shouldBeEmpty()
        }

        test("are an error when given both by name and by position") {
            val reading = reading32(mediaTypes("$NAMED, $PREFIX", "$NAMED, $ITEM"))
            val location = reading.diagnostics.first().location.shouldNotBeNull()

            reading.codes() shouldBe List(2) { DiagnosticCodes.MEDIA_TYPE_ENCODING }
            location.pointer.toString() shouldBe "/components/mediaTypes/M0"
        }
    }

    context("the encodings of a nested part") {
        test("are an error when given both by name and by position") {
            val reading = reading32(mediaTypes(ITEM, "itemEncoding: {$NAMED, $PREFIX}"))
            val location = reading.diagnostics.single().location.shouldNotBeNull()

            reading.codes() shouldBe listOf(DiagnosticCodes.MEDIA_TYPE_ENCODING)
            location.pointer.toString() shouldBe "/components/mediaTypes/M1/itemEncoding"
        }
    }
})
