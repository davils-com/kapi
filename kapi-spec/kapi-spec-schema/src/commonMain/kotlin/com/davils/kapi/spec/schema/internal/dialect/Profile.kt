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

internal class Profile(
    val draft: Draft,
    val vocabularies: Set<Vocabulary>,
    val formatAssertion: Boolean,
    val dialect: Dialect?,
) {
    fun enables(vocabulary: Vocabulary?): Boolean {
        if (vocabulary == null || !draft.hasVocabularies) return true
        val isMerged = draft == Draft.Draft201909 && vocabulary == Vocabulary.Unevaluated
        return (Vocabulary.Applicator.takeIf { isMerged } ?: vocabulary) in vocabularies
    }

    override fun toString(): String = "Profile($draft, $vocabularies, formatAssertion=$formatAssertion)"
}
