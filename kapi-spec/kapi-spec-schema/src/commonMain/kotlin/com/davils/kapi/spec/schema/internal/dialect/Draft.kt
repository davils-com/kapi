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

internal enum class Draft(val idKeyword: String?) {
    Draft04("id"),
    Draft06("\$id"),
    Draft07("\$id"),
    Draft201909("\$id"),
    Draft202012("\$id"),
    OpenApi30(null),
    ;

    val refOverridesSiblings: Boolean get() = this in LEGACY

    val allowsBooleanSchemas: Boolean get() = this != Draft04 && this != OpenApi30

    val hasVocabularies: Boolean get() = this == Draft201909 || this == Draft202012

    val anchorsInId: Boolean get() = this == Draft04 || this == Draft06 || this == Draft07

    private companion object {
        val LEGACY: Set<Draft> = setOf(Draft04, Draft06, Draft07, OpenApi30)
    }
}
