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

package com.davils.kapi.spec.asyncapi.read.shape

import com.davils.kaml.core.model.YamlTag
import com.davils.kaml.core.node.YamlNode

internal enum class FieldKind(val label: String, private val accepts: (YamlNode) -> Boolean) {
    Text("a string", { node -> tagged(node, YamlTag.STRING) }),
    TextOrNull("a string or null", { node -> tagged(node, YamlTag.STRING) || tagged(node, YamlTag.NULL) }),
    Mapping("an object", { node -> node is YamlNode.Mapping }),
    Sequence("an array", { node -> node is YamlNode.Sequence }),
    Schema("a schema", { node -> node is YamlNode.Mapping || tagged(node, YamlTag.BOOLEAN) }),
    Value("a value", { true }),
    ;

    fun accepts(node: YamlNode): Boolean = accepts.invoke(node)

    private companion object {
        fun tagged(node: YamlNode, tag: YamlTag): Boolean {
            val scalar = node as? YamlNode.Scalar
            return scalar?.tag == tag
        }
    }
}
