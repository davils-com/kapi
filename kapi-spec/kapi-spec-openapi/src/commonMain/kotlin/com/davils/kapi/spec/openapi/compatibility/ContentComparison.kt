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
import com.davils.kapi.spec.core.compatibility.Direction
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.openapi.model.content.MediaType
import com.davils.kapi.spec.openapi.model.content.RequestBody
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.parameter.Header
import com.davils.kapi.spec.openapi.model.path.Operation
import com.davils.kapi.spec.openapi.resolve
import com.davils.kore.core.annotation.DavilsInternal

internal class ContentComparison(private val context: Context) {
    private val requests: Media = Media(
        removed = "request-media-type-removed",
        added = "request-media-type-added",
        direction = Direction.Request,
    )

    private val replies: Media = Media(
        removed = "response-media-type-removed",
        added = "response-media-type-added",
        direction = Direction.Response,
    )

    fun request(name: String, operations: Both<Operation>) {
        val before = operations.before.requestBody?.let { body -> context.baseline.resolve<RequestBody>(body) }
        val after = operations.after.requestBody?.let { body -> context.candidate.resolve<RequestBody>(body) }
        val label = "The request body of $name"
        if (before == null && after != null) bodyAdded(label = label, body = after)
        if (before != null && after == null) {
            context.report(
                rule = "request-body-removed",
                impacts = clientBreaking,
                message = "$label is removed",
                before = before,
                after = null,
            )
        }
        if (before != null && after != null) body(label = label, bodies = Both(before = before, after = after))
    }

    fun responses(name: String, operations: Both<Operation>) {
        val old = operations.before.responses.entries
        val new = operations.after.responses.entries
        old.forEach { (status, referable) ->
            val label = "The response $status of $name"
            val before = context.baseline.resolve<Response>(referable)
            val found = new[status]
            val after = found?.let { candidate -> context.candidate.resolve<Response>(candidate) }
            if (found == null) {
                context.report(
                    rule = "response-status-removed",
                    impacts = clientBreaking,
                    message = "$label is removed",
                    before = before,
                    after = null,
                )
            }
            if (before != null && after != null) {
                response(label = label, responses = Both(before = before, after = after))
            }
        }
        new.forEach { (status, referable) ->
            if (status in old) return@forEach
            context.report(
                rule = "response-status-added",
                impacts = neitherBreaking,
                message = "The response $status of $name is added",
                before = null,
                after = context.candidate.resolve<Response>(referable),
            )
        }
    }

    private fun bodyAdded(label: String, body: RequestBody) {
        val impacts = clientBreaking.takeIf { body.isRequired } ?: neitherBreaking
        val required = " as required".takeIf { body.isRequired }.orEmpty()
        context.report(
            rule = "request-body-added",
            impacts = impacts,
            message = "$label is added$required",
            before = null,
            after = body,
        )
    }

    private fun body(label: String, bodies: Both<RequestBody>) {
        val before = bodies.before
        val after = bodies.after
        if (after.isRequired && !before.isRequired) {
            context.report(
                rule = "request-body-required",
                impacts = clientBreaking,
                message = "$label is required",
                before = before,
                after = after,
            )
        }
        if (before.isRequired && !after.isRequired) {
            context.report(
                rule = "request-body-optional",
                impacts = providerBreaking,
                message = "$label is optional",
                before = before,
                after = after,
            )
        }
        val owners = Both<Element>(before = before, after = after)
        val contents = Both(before = before.content, after = after.content)
        media(label = label, contents = contents, owners = owners, media = requests)
    }

    private fun response(label: String, responses: Both<Response>) {
        val before = responses.before
        val after = responses.after
        val owners = Both<Element>(before = before, after = after)
        val contents = Both(before = before.content, after = after.content)
        media(label = label, contents = contents, owners = owners, media = replies)
        headers(label = label, responses = responses)
    }

    private fun headers(label: String, responses: Both<Response>) {
        val old = keyedByLowercase(responses.before.headers)
        val new = keyedByLowercase(responses.after.headers)
        old.forEach { (header, referable) ->
            val before = context.baseline.resolve<Header>(referable)
            val found = new[header]
            val after = found?.let { candidate -> context.candidate.resolve<Header>(candidate) }
            if (found == null) {
                context.report(
                    rule = "response-header-removed",
                    impacts = clientBreaking,
                    message = "$label no longer sends the header ${shown(header)}",
                    before = before,
                    after = null,
                )
            }
            if (before != null && after != null) {
                context.schemas(before = before.schema, after = after.schema, direction = Direction.Response)
            }
        }
    }

    private fun media(
        label: String,
        contents: Both<Map<String, Referable<MediaType>>>,
        owners: Both<Element>,
        media: Media,
    ) {
        val old = keyedByLowercase(contents.before)
        val new = keyedByLowercase(contents.after)
        old.forEach { (type, referable) ->
            val found = new[type]
            if (found == null) {
                context.report(
                    rule = media.removed,
                    impacts = clientBreaking,
                    message = "$label no longer carries ${shown(type)}",
                    before = owners.before,
                    after = owners.after,
                )
            }
            val before = context.baseline.resolve<MediaType>(referable)
            val after = found?.let { candidate -> context.candidate.resolve<MediaType>(candidate) }
            if (before != null && after != null) {
                context.schemas(before = before.schema, after = after.schema, direction = media.direction)
            }
        }
        new.keys.filterNot { type -> type in old }.forEach { type ->
            context.report(
                rule = media.added,
                impacts = providerBreaking,
                message = "$label now also carries ${shown(type)}",
                before = owners.before,
                after = owners.after,
            )
        }
    }

    private fun <T : Any> keyedByLowercase(entries: Map<String, T>): Map<String, T> =
        entries.entries.associateKeyed { (key, value) -> key.lowercase() to value }

    private data class Media(val removed: String, val added: String, val direction: Direction)
}
