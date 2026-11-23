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

package com.davils.kapi.spec.core.model.fixture

import com.davils.kapi.spec.core.model.ElementBuilder
import com.davils.kapi.spec.core.model.Referable

internal class NodeBuilder : ElementBuilder<Node>() {
    var name: String = ""

    val items: MutableList<Node> = mutableListOf()

    val named: MutableMap<String, Node> = linkedMapOf()

    var referable: Referable<Node>? = null

    override fun assemble(): Node = Node(this)
}

internal fun node(name: String, configure: NodeBuilder.() -> Unit = {}): Node {
    val builder = NodeBuilder()
    builder.name = name
    builder.configure()
    return builder.build()
}
