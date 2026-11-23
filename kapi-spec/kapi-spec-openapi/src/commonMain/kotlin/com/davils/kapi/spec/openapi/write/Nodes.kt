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

package com.davils.kapi.spec.openapi.write

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlNull
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kaml.core.node.yamlTagged
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value

internal object Nodes {
    private val integer: Regex = Regex("-?[0-9]+")

    fun of(value: Value): YamlNode = when (value) {
        is Value.Null -> yamlNull()
        is Value.Flag -> yamlScalar(value.isTrue)
        is Value.Number -> of(value.value)
        is Value.Text -> yamlScalar(value.value)
        is Value.Sequence -> yamlSequence(value.items.map(::of))
        is Value.Mapping -> mapping(value.entries, ::of)
    }

    fun of(number: NumberValue): YamlNode {
        val tag = YamlTag.INTEGER.takeIf { integer.matches(number.literal) } ?: YamlTag.FLOAT
        return yamlTagged(number.literal, tag)
    }

    fun texts(values: List<String>): YamlNode = yamlSequence(values.map { value -> yamlScalar(value) })

    fun <T> mapping(values: Map<String, T>, write: (T) -> YamlNode): YamlNode =
        yamlMapping(values.map { (key, value) -> YamlEntry(yamlScalar(key), write(value)) })
}
