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

package com.davils.kapi.spec.schema.internal.meta

private const val JSON_SCHEMA: String = "http://json-schema.org/"

private const val DRAFT_2019_09: String = JSON_SCHEMA + "draft/2019-09/"

private const val DRAFT_2020_12: String = JSON_SCHEMA + "draft/2020-12/"

private const val OPENAPI_31: String = "https://spec.openapis.org/oas/3.1/"

internal object MetaSchemas {
    private val texts: Map<String, String> = mapOf(
        JSON_SCHEMA + "draft-04/schema" to DRAFT_04_SCHEMA,
        JSON_SCHEMA + "draft-06/schema" to DRAFT_06_SCHEMA,
        JSON_SCHEMA + "draft-07/schema" to DRAFT_07_SCHEMA,
        DRAFT_2019_09 + "schema" to DRAFT_2019_09_SCHEMA,
        DRAFT_2019_09 + "meta/core" to DRAFT_2019_09_CORE,
        DRAFT_2019_09 + "meta/applicator" to DRAFT_2019_09_APPLICATOR,
        DRAFT_2019_09 + "meta/validation" to DRAFT_2019_09_VALIDATION,
        DRAFT_2019_09 + "meta/meta-data" to DRAFT_2019_09_META_DATA,
        DRAFT_2019_09 + "meta/format" to DRAFT_2019_09_FORMAT,
        DRAFT_2019_09 + "meta/content" to DRAFT_2019_09_CONTENT,
        DRAFT_2020_12 + "schema" to DRAFT_2020_12_SCHEMA,
        DRAFT_2020_12 + "meta/core" to DRAFT_2020_12_CORE,
        DRAFT_2020_12 + "meta/applicator" to DRAFT_2020_12_APPLICATOR,
        DRAFT_2020_12 + "meta/unevaluated" to DRAFT_2020_12_UNEVALUATED,
        DRAFT_2020_12 + "meta/validation" to DRAFT_2020_12_VALIDATION,
        DRAFT_2020_12 + "meta/meta-data" to DRAFT_2020_12_META_DATA,
        DRAFT_2020_12 + "meta/format-annotation" to DRAFT_2020_12_FORMAT_ANNOTATION,
        DRAFT_2020_12 + "meta/format-assertion" to DRAFT_2020_12_FORMAT_ASSERTION,
        DRAFT_2020_12 + "meta/content" to DRAFT_2020_12_CONTENT,
        OPENAPI_31 + "dialect/base" to OPENAPI_31_DIALECT,
        OPENAPI_31 + "meta/base" to OPENAPI_31_VOCABULARY,
    )

    val uris: Set<String> get() = texts.keys

    fun textOf(uri: String): String? {
        val bare = uri.removeSuffix("#")
        return texts[bare.replace(oldValue = "https://json-schema.org/", newValue = JSON_SCHEMA)]
    }
}
