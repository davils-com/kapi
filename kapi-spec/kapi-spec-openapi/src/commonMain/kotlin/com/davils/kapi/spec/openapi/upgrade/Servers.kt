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
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class Servers(
    private val host: String?,
    private val basePath: String?,
) {
    fun ofDocument(schemes: List<String>): YamlNode? = sequenceOf(urlsOf(schemes))

    fun ofOperation(schemes: List<String>): YamlNode? {
        if (host == null) return null
        return sequenceOf(urlsOf(schemes))
    }

    private fun urlsOf(schemes: List<String>): List<String> {
        val base = basePath.orEmpty()
        val hosted = host?.let { name -> hostedOf(name, base, schemes) }
        return hosted ?: listOf(base).filter(String::isNotEmpty)
    }

    private fun hostedOf(host: String, base: String, schemes: List<String>): List<String> {
        val urls = schemes.map { scheme -> "$scheme://$host$base" }
        return urls.ifEmpty { listOf("//$host$base") }
    }

    private fun sequenceOf(urls: List<String>): YamlNode? {
        if (urls.isEmpty()) return null
        val servers = urls.map { url -> Trees.mappingOf(Trees.entries(Keys.URL to Trees.textOf(url))) }
        return Trees.sequenceOf(servers)
    }
}
