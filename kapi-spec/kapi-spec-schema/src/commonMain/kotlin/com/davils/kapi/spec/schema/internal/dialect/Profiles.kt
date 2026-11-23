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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object Profiles {
    private val drafts: Map<Dialect, Draft> = mapOf(
        Dialect.Draft04 to Draft.Draft04,
        Dialect.Draft06 to Draft.Draft06,
        Dialect.Draft07 to Draft.Draft07,
        Dialect.Draft201909 to Draft.Draft201909,
        Dialect.Draft202012 to Draft.Draft202012,
        Dialect.OpenApi30 to Draft.OpenApi30,
        Dialect.OpenApi31 to Draft.Draft202012,
    )

    fun of(dialect: Dialect, formatAssertion: Boolean): Profile {
        val draft = drafts.getValue(dialect)
        val vocabularies = when (dialect) {
            Dialect.Draft201909 -> Vocabulary.DEFAULT_201909
            Dialect.Draft202012 -> Vocabulary.DEFAULT_202012
            Dialect.OpenApi31 -> Vocabulary.DEFAULT_202012 + Vocabulary.OpenApiBase
            Dialect.Draft04, Dialect.Draft06, Dialect.Draft07, Dialect.OpenApi30 -> emptySet()
        }
        return Profile(draft, vocabularies, formatAssertion, dialect)
    }

    fun dialectOf(uri: String): Dialect? {
        val key = keyOf(uri)
        return Dialect.entries.firstOrNull { dialect -> dialect.uri?.let(::keyOf) == key }
    }

    fun custom(meta: JsonElement, metaProfile: Profile, formatAssertion: Boolean, unknown: (String) -> Unit): Profile {
        val declared = (meta as? JsonObject)?.get("\$vocabulary") as? JsonObject
        val usable = declared.takeIf { metaProfile.draft.hasVocabularies } ?: return metaProfile
        val vocabularies = LinkedHashSet<Vocabulary>()
        usable.forEach { (uri, required) ->
            val vocabulary = Vocabulary.of(uri, metaProfile.draft)
            vocabulary?.let(vocabularies::add)
            val isRequired = (required as? JsonPrimitive)?.content == "true"
            if (vocabulary == null && isRequired) unknown(uri)
        }
        val asserts = formatAssertion || Vocabulary.FormatAssertion in vocabularies
        return Profile(metaProfile.draft, vocabularies, asserts, dialect = null)
    }

    private fun keyOf(uri: String): String {
        val bare = uri.removeSuffix("#")
        return bare.replace("https://json-schema.org/", "http://json-schema.org/")
    }
}
