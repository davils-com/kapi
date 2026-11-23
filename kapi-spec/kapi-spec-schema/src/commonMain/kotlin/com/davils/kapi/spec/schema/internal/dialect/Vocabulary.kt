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

private const val DRAFT_2019_09: String = "https://json-schema.org/draft/2019-09/vocab/"

private const val DRAFT_2020_12: String = "https://json-schema.org/draft/2020-12/vocab/"

internal enum class Vocabulary(val uri201909: String?, val uri202012: String?) {
    Core(DRAFT_2019_09 + "core", DRAFT_2020_12 + "core"),
    Applicator(DRAFT_2019_09 + "applicator", DRAFT_2020_12 + "applicator"),
    Unevaluated(null, DRAFT_2020_12 + "unevaluated"),
    Validation(DRAFT_2019_09 + "validation", DRAFT_2020_12 + "validation"),
    MetaData(DRAFT_2019_09 + "meta-data", DRAFT_2020_12 + "meta-data"),
    FormatAnnotation(DRAFT_2019_09 + "format", DRAFT_2020_12 + "format-annotation"),
    FormatAssertion(null, DRAFT_2020_12 + "format-assertion"),
    Content(DRAFT_2019_09 + "content", DRAFT_2020_12 + "content"),
    OpenApiBase(null, "https://spec.openapis.org/oas/3.1/vocab/base"),
    ;

    companion object {
        fun of(uri: String, draft: Draft): Vocabulary? {
            val is201909 = draft == Draft.Draft201909
            return entries.firstOrNull { vocabulary ->
                val own = vocabulary.uri201909.takeIf { is201909 } ?: vocabulary.uri202012.takeIf { !is201909 }
                own == uri
            }
        }

        val DEFAULT_201909: Set<Vocabulary> = setOf(Core, Applicator, Validation, MetaData, FormatAnnotation, Content)

        val DEFAULT_202012: Set<Vocabulary> =
            setOf(Core, Applicator, Unevaluated, Validation, MetaData, FormatAnnotation, Content)
    }
}
