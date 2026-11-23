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

package com.davils.kapi.spec.openapi.compatibility

import com.davils.kapi.spec.core.collection.associateKeyed
import com.davils.kapi.spec.core.compatibility.ChangeLog
import com.davils.kapi.spec.core.compatibility.Comparison
import com.davils.kapi.spec.core.compatibility.ComparisonSettings
import com.davils.kapi.spec.core.compatibility.Impact
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.model.path.PathItem
import com.davils.kapi.spec.openapi.model.path.PathTemplate
import com.davils.kapi.spec.openapi.resolve
import com.davils.kore.core.annotation.DavilsInternal

private const val PREFIX: String = "openapi"

private const val PLACEHOLDER: String = "{}"

internal class DocumentComparison(baseline: Document, candidate: Document, settings: ComparisonSettings) {
    private val context: Context =
        Context(baseline = baseline, candidate = candidate, log = ChangeLog(prefix = PREFIX, settings = settings))

    private val operations: OperationComparison = OperationComparison(context)

    fun compare(): Comparison {
        servers()
        paths()
        SecurityComparison.schemes(context)
        return context.log.comparison()
    }

    private fun servers() {
        val old = context.baseline.servers.associateKeyed { server -> server.url to server }
        val new = context.candidate.servers.associateKeyed { server -> server.url to server }
        old.forEach { (url, server) ->
            val message = "The server ${shown(url)} is removed"
            if (url !in new) {
                report(
                    rule = "server-removed",
                    impacts = clientBreaking,
                    message = message,
                    before = server,
                    after = null,
                )
            }
        }
        new.forEach { (url, server) ->
            val message = "The server ${shown(url)} is added"
            if (url !in old) {
                report(
                    rule = "server-added",
                    impacts = neitherBreaking,
                    message = message,
                    before = null,
                    after = server,
                )
            }
        }
    }

    private fun paths() {
        val old = shapesOf(context.baseline)
        val new = shapesOf(context.candidate)
        old.forEach { (shape, before) ->
            val after = new[shape]
            if (after == null) removed(before)
            after?.let { found -> path(Both(before = before, after = found)) }
        }
        new.forEach { (shape, after) -> if (shape !in old) added(after) }
    }

    private fun removed(path: Path) {
        val message = "The path ${shown(path.template.value)} is removed"
        report(rule = "path-removed", impacts = clientBreaking, message = message, before = path.item, after = null)
    }

    private fun added(path: Path) {
        val message = "The path ${shown(path.template.value)} is added"
        report(rule = "path-added", impacts = providerBreaking, message = message, before = null, after = path.item)
    }

    private fun path(paths: Both<Path>) {
        val before = paths.before
        val after = paths.after
        if (before.template != after.template) {
            val message = "The path ${shown(before.template.value)} names its parameters differently as " +
                shown(after.template.value)
            report(
                rule = "path-parameter-renamed",
                impacts = neitherBreaking,
                message = message,
                before = before.item,
                after = after.item,
            )
        }
        val old = operationsOf(before.item)
        val new = operationsOf(after.item)
        old.forEach { (method, operation) ->
            val name = "$method ${shown(after.template.value)}"
            val found = new[method]
            if (found == null) operations.removed(name = name, operation = operation)
            found?.let { candidate -> operations.compare(name, paths, Both(before = operation, after = candidate)) }
        }
        new.forEach { (method, operation) ->
            if (method !in old) operations.added(name = "$method ${shown(after.template.value)}", operation = operation)
        }
    }

    private fun shapesOf(document: Document): Map<String, Path> {
        val items = document.paths?.items.orEmpty()
        return items.entries.associateKeyed { (template, item) ->
            val target = item.reference?.let { reference -> document.resolve(reference) as? PathItem }
            shapeOf(template) to Path(template = template, item = target ?: item)
        }
    }

    private fun operationsOf(item: PathItem): Map<String, Operation> {
        val fixed = item.operations.entries.map { (method, operation) -> method.keyword.uppercase() to operation }
        val additional = item.additionalOperations.entries.map { (method, operation) -> method to operation }
        return (fixed + additional).associateKeyed { pair -> pair }
    }

    private fun shapeOf(template: PathTemplate): String {
        val text = StringBuilder()
        var isInside = false
        template.value.forEach { character ->
            val isOpening = character == '{'
            val isClosing = character == '}'
            if (isOpening) text.append(PLACEHOLDER)
            if (!isOpening && !isClosing && !isInside) text.append(character)
            isInside = (isInside || isOpening) && !isClosing
        }
        return text.toString()
    }

    private fun report(
        rule: String,
        impacts: Pair<Impact, Impact>,
        message: String,
        before: Element?,
        after: Element?,
    ) {
        context.report(rule = rule, impacts = impacts, message = message, before = before, after = after)
    }

    data class Path(val template: PathTemplate, val item: PathItem)
}
