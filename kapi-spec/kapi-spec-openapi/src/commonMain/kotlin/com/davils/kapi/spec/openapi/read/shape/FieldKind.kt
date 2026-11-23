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

package com.davils.kapi.spec.openapi.read.shape

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode
import com.davils.kapi.spec.core.value.NumberValue

internal enum class FieldKind(val label: String, private val accepts: (YamlNode) -> Boolean) {
    Text("a string", { node -> isText(node) }),
    Flag("a boolean", { node -> tagged(node, YamlTag.BOOLEAN) }),
    Integer("an integer that is not negative", { node -> count(node) != null }),
    Number("a number", { node -> numeric(node) != null }),
    Mapping("an object", { node -> node is YamlNode.Mapping }),
    Sequence("an array", { node -> node is YamlNode.Sequence }),
    Schema("a schema", { node -> node is YamlNode.Mapping || tagged(node, YamlTag.BOOLEAN) }),
    TextOrSequence("a string or an array", { node -> isText(node) || node is YamlNode.Sequence }),
    Value("a value", { true }),
    ;

    fun accepts(node: YamlNode): Boolean = accepts.invoke(node)

    companion object {
        fun count(node: YamlNode): Int? {
            val number = numeric(node)?.takeIf { it.isIntegral }
            return number?.toIntOrNull()?.takeIf { it >= 0 }
        }

        private fun isText(node: YamlNode): Boolean = tagged(node, YamlTag.STRING)

        private fun tagged(node: YamlNode, tag: YamlTag): Boolean {
            val scalar = node as? YamlNode.Scalar
            return scalar?.tag == tag
        }

        fun numeric(node: YamlNode): NumberValue? {
            val isNumeric = tagged(node, YamlTag.INTEGER) || tagged(node, YamlTag.FLOAT)
            val scalar = (node as? YamlNode.Scalar)?.takeIf { isNumeric }
            return scalar?.let { NumberValue.parseOrNull(it.value) }
        }
    }
}
