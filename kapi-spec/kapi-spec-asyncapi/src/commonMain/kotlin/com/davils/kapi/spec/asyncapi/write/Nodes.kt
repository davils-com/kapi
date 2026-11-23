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

package com.davils.kapi.spec.asyncapi.write

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
    fun of(value: Value): YamlNode = when (value) {
        is Value.Null -> yamlNull()
        is Value.Flag -> yamlScalar(value.isTrue)
        is Value.Number -> of(value.value)
        is Value.Text -> yamlScalar(value.value)
        is Value.Sequence -> yamlSequence(value.items.map(::of))
        is Value.Mapping -> mapping(value.entries, ::of)
    }

    fun texts(values: List<String>): YamlNode = yamlSequence(values.map { value -> yamlScalar(value) })

    fun <T> mapping(values: Map<String, T>, write: (T) -> YamlNode): YamlNode =
        yamlMapping(values.map { (key, value) -> YamlEntry(key = yamlScalar(key), value = write(value)) })

    private fun of(number: NumberValue): YamlNode {
        val literal = number.literal
        val magnitude = literal.removePrefix("-")
        val isInteger = magnitude.isNotEmpty() && magnitude.all { character -> character in '0'..'9' }
        val tag = YamlTag.INTEGER.takeIf { isInteger } ?: YamlTag.FLOAT
        return yamlTagged(literal, tag)
    }
}
