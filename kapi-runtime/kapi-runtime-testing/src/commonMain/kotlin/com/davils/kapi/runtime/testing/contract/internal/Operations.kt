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

package com.davils.kapi.runtime.testing.contract.internal

import com.davils.kapi.runtime.testing.internal.PathMatch
import com.davils.kapi.runtime.testing.internal.PathTemplates
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.path.Method
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.resolve

private const val SCHEME_END: String = "://"

internal class Operations(document: Document, basePath: String) {
    private val items: List<Pair<String, PathItem>> = document.paths?.items.orEmpty().map { (template, item) ->
        val referenced = item.reference?.let { reference -> document.resolve(reference) as? PathItem }
        template.value to (referenced ?: item)
    }

    private val prefixes: List<String> = run {
        val servers = document.servers.mapNotNull { server -> pathOf(server.url) }
        (listOf(basePath) + servers).distinct()
    }

    fun match(method: String, path: String): OperationMatch {
        val candidates = prefixes.mapNotNull { prefix -> below(path, prefix) }
        val matches = candidates.flatMap { candidate ->
            items.mapNotNull { (template, item) ->
                val found = PathTemplates.match(template = template, path = candidate)
                found?.let { match -> match to item }
            }
        }
        val best = matches.maxByOrNull { (found, _) -> found.literals } ?: return OperationMatch.Unknown
        return found(method = method, match = best.first, item = best.second)
    }

    private fun found(method: String, match: PathMatch, item: PathItem): OperationMatch {
        val standard = Method.fromKeyword(method.lowercase()).takeIf { method.none(Char::isLowerCase) }
        val operation = standard?.let(item.operations::get) ?: item.additionalOperations[method]
        return operation?.let { declared ->
            OperationMatch.Found(
                template = match.template,
                item = item,
                operation = declared,
                parameters = match.parameters,
            )
        } ?: OperationMatch.PathOnly(template = match.template, item = item)
    }

    private fun below(path: String, prefix: String): String? {
        val rest = path.removePrefix(prefix)
        val isBelow = path.startsWith(prefix) && (rest.isEmpty() || rest.startsWith('/'))
        return rest.ifEmpty { "/" }.takeIf { isBelow }
    }

    private fun pathOf(url: String): String? {
        val isAbsolute = SCHEME_END in url
        val rest = url.substringAfter(SCHEME_END)
        val path = ("/" + rest.substringAfter(delimiter = '/', missingDelimiterValue = "")).takeIf { isAbsolute } ?: url
        val trimmed = path.substringBefore('?').trimEnd('/')
        return trimmed.takeIf { found -> found.startsWith('/') && '{' !in found }
    }
}
