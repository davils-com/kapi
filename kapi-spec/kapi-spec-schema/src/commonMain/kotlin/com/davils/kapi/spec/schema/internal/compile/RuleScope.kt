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

package com.davils.kapi.spec.schema.internal.compile

import com.davils.kapi.spec.schema.internal.dialect.Draft
import com.davils.kapi.spec.schema.internal.dialect.Vocabulary
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import kotlinx.serialization.json.JsonElement

internal class RuleScope(private val rules: MutableList<KeywordRule>, private val vocabulary: Vocabulary?) {
    fun keyword(name: String, drafts: Set<Draft>, compile: (KeywordContext, JsonElement) -> Keyword?) {
        add(name = name, drafts = drafts, shape = Shape.None, compile = compile)
    }

    fun single(name: String, drafts: Set<Draft>, compile: ((KeywordContext, JsonElement) -> Keyword?)? = null) {
        add(name = name, drafts = drafts, shape = Shape.Single, compile = compile)
    }

    fun list(name: String, drafts: Set<Draft>, compile: (KeywordContext, JsonElement) -> Keyword?) {
        add(name = name, drafts = drafts, shape = Shape.List, compile = compile)
    }

    fun map(name: String, drafts: Set<Draft>, compile: ((KeywordContext, JsonElement) -> Keyword?)? = null) {
        add(name = name, drafts = drafts, shape = Shape.Map, compile = compile)
    }

    fun singleOrList(name: String, drafts: Set<Draft>, compile: (KeywordContext, JsonElement) -> Keyword?) {
        add(name = name, drafts = drafts, shape = Shape.SingleOrList, compile = compile)
    }

    fun dependencies(name: String, drafts: Set<Draft>, compile: (KeywordContext, JsonElement) -> Keyword?) {
        add(name = name, drafts = drafts, shape = Shape.Dependencies, compile = compile)
    }

    private fun add(
        name: String,
        drafts: Set<Draft>,
        shape: Shape,
        compile: ((KeywordContext, JsonElement) -> Keyword?)?,
    ) {
        rules += KeywordRule(name = name, drafts = drafts, vocabulary = vocabulary, shape = shape, compile = compile)
    }
}
