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

package com.davils.kapi.spec.openapi.model.schema

import com.davils.kapi.spec.core.model.Element

public class Xml internal constructor(builder: XmlBuilder) : Element(builder) {
    public val name: String? = builder.name

    public val namespace: String? = builder.namespace

    public val prefix: String? = builder.prefix

    public val isAttribute: Boolean = builder.isAttribute

    public val isWrapped: Boolean = builder.isWrapped

    override val identity: List<Any?> get() = listOf(name, namespace, prefix, isAttribute, isWrapped)
}
