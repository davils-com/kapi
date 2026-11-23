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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kapi.spec.openapi.read.shape.Keys

private val flows: Map<String, String> = mapOf(
    "implicit" to Keys.IMPLICIT,
    "password" to Keys.PASSWORD,
    "application" to Keys.CLIENT_CREDENTIALS,
    "accessCode" to Keys.AUTHORIZATION_CODE,
)

private val flowKeys: Set<String> = setOf(Keys.AUTHORIZATION_URL, Keys.TOKEN_URL, Keys.SCOPES)

private val descriptionKeys: Set<String> = setOf(Keys.DESCRIPTION)

internal object SecurityUpgrade {
    private val upgrades: Map<String, (YamlNode) -> YamlNode> by lazy {
        mapOf(SwaggerKeys.BASIC to ::basic, SwaggerKeys.OAUTH2 to ::oauth)
    }

    fun upgraded(scheme: YamlNode): YamlNode {
        val upgrade = scheme[Keys.TYPE].string()?.let(upgrades::get) ?: return scheme
        return upgrade(scheme)
    }

    private fun basic(scheme: YamlNode): YamlNode {
        val head = Trees.entries(Keys.TYPE to Trees.textOf(Keys.HTTP), Keys.SCHEME to Trees.textOf(SwaggerKeys.BASIC))
        val described = Trees.only(scheme, descriptionKeys)
        return Trees.mappingAt(scheme, head + described + Trees.extensions(scheme))
    }

    private fun oauth(scheme: YamlNode): YamlNode {
        val flow = scheme[SwaggerKeys.FLOW].string()?.let(flows::get) ?: return scheme
        val settings = Trees.mappingOf(Trees.only(scheme, flowKeys))
        val declared = Trees.mappingOf(Trees.entries(flow to settings))
        val head = Trees.entries(Keys.TYPE to Trees.textOf(SwaggerKeys.OAUTH2))
        val body = Trees.only(scheme, descriptionKeys) + Trees.entries(Keys.FLOWS to declared)
        return Trees.mappingAt(scheme, head + body + Trees.extensions(scheme))
    }
}
