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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kaml.core.node.string
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.load.LoadedDocument
import com.davils.kapi.spec.openapi.diagnostic.Findings
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kore.core.annotation.DavilsInternal

private val consumed: Set<String> = setOf(
    Keys.SWAGGER,
    Keys.OPENAPI,
    Keys.INFO,
    SwaggerKeys.HOST,
    SwaggerKeys.BASE_PATH,
    SwaggerKeys.SCHEMES,
    SwaggerKeys.CONSUMES,
    SwaggerKeys.PRODUCES,
    Keys.PATHS,
    SwaggerKeys.DEFINITIONS,
    Keys.PARAMETERS,
    Keys.RESPONSES,
    SwaggerKeys.SECURITY_DEFINITIONS,
)

private val infoKeys: Set<String> = setOf(Keys.INFO)

internal class SwaggerUpgrade(private val findings: Findings) {
    fun upgraded(document: LoadedDocument): LoadedDocument {
        val root = (document.root as? YamlNode.Mapping)?.takeIf(::isSwagger) ?: return document
        val notes = UpgradeNotes(document, findings)
        notes.upgraded(root)
        return LoadedDocument(document.text, rootOf(root, UpgradeContext(root, notes)), document.source)
    }

    private fun isSwagger(root: YamlNode.Mapping): Boolean = root[Keys.SWAGGER].string() == SwaggerKeys.VERSION

    private fun rootOf(root: YamlNode.Mapping, context: UpgradeContext): YamlNode.Mapping {
        val schemes = Trees.texts(root[SwaggerKeys.SCHEMES]).orEmpty()
        val version = Trees.entries(Keys.OPENAPI to Trees.textOf(SwaggerKeys.UPGRADED_VERSION))
        val body = Trees.entries(
            Keys.SERVERS to context.servers.ofDocument(schemes),
            Keys.PATHS to root[Keys.PATHS]?.let { paths -> pathsOf(paths, PathUpgrade(context)) },
            Keys.COMPONENTS to ComponentUpgrade(context).upgraded(root),
        )
        return Trees.mappingAt(root, version + Trees.only(root, infoKeys) + body + Trees.without(root, consumed))
    }

    private fun pathsOf(paths: YamlNode, upgrade: PathUpgrade): YamlNode = Trees.mapped(paths) { path, item ->
        upgrade.upgraded(Located(item, JsonPointer.of(Keys.PATHS, path)))
    }
}
