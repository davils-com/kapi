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

package com.davils.kapi.spec.schema.internal.dialect

import com.davils.kapi.spec.schema.Dialect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json

private const val CORE: String = "https://json-schema.org/draft/2020-12/vocab/core"

class ProfilesTest : FunSpec({
    test("a custom meta-schema without vocabularies keeps the profile of its own meta-schema") {
        val modern = Profiles.of(Dialect.Draft202012, isFormatAsserted = false)
        val unknown = ArrayList<String>()
        Profiles.custom(Json.parseToJsonElement("true"), modern, isFormatAsserted = false) { unknown += it } shouldBe
            modern
        val listed = Json.parseToJsonElement("""{"${'$'}vocabulary": ["$CORE"]}""")
        Profiles.custom(listed, modern, isFormatAsserted = false) { unknown += it } shouldBe modern
        val legacy = Profiles.of(Dialect.Draft07, isFormatAsserted = false)
        val declared = Json.parseToJsonElement("""{"${'$'}vocabulary": {"$CORE": true}}""")
        Profiles.custom(declared, legacy, isFormatAsserted = false) { unknown += it } shouldBe legacy
        unknown.shouldBeEmpty()
    }

    test("a vocabulary is required only by the value true") {
        val modern = Profiles.of(Dialect.Draft202012, isFormatAsserted = true)
        val unknown = ArrayList<String>()
        val meta = Json.parseToJsonElement(
            """{"${'$'}vocabulary": {"$CORE": true, "https://example.com/a": {}, "https://example.com/b": true}}""",
        )
        val profile = Profiles.custom(meta, modern, isFormatAsserted = true) { unknown += it }
        profile.vocabularies shouldBe setOf(Vocabulary.Core)
        profile.isFormatAsserted shouldBe true
        profile.toString() shouldContain "isFormatAsserted=true"
        unknown shouldBe listOf("https://example.com/b")
    }

    test("a draft knows its rules") {
        Draft.entries.map { it.areBooleanSchemasAllowed } shouldBe listOf(false, true, true, true, true, false)
        Draft.entries.map { it.hasRefOverridingSiblings } shouldBe listOf(true, true, true, false, false, true)
        val named = Dialect.entries.filter { it.uri != null }
        named.map { Profiles.dialectOf(it.uri.orEmpty()) } shouldBe named
    }
})
