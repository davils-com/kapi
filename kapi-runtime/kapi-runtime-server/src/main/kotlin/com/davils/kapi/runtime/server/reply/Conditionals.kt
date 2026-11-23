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

package com.davils.kapi.runtime.server.reply

import com.davils.kapi.runtime.core.conditional.PreconditionDecision
import com.davils.kapi.runtime.core.conditional.Preconditions
import com.davils.kapi.runtime.core.conditional.RepresentationState
import com.davils.kapi.runtime.server.internal.Statuses
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.request.Request

private const val FIRST_SUCCESS: Int = 200

private const val LAST_SUCCESS: Int = 299

/**
 * Reads the preconditions of RFC 9110 §13.1 of this request: `If-Match`, `If-None-Match`, `If-Modified-Since` and
 * `If-Unmodified-Since`, within the field limits of the server.
 *
 * A malformed list of entity tags matches nothing and a malformed date is ignored, as RFC 9110 demands.
 *
 * @receiver the request.
 * @return the preconditions.
 * @since 1.0.0
 */
public fun Request.preconditions(): Preconditions = Preconditions.read(
    ifMatch = joined(this, "if-match"),
    ifNoneMatch = joined(this, "if-none-match"),
    ifModifiedSince = headers["if-modified-since"],
    ifUnmodifiedSince = headers["if-unmodified-since"],
    now = context.now(),
    limits = context.limits.fields,
)

/**
 * Answers this request conditionally by RFC 9110 §13.2.2: [produce] runs only when every precondition holds.
 *
 * A failed `If-Match` or `If-Unmodified-Since`, and a matching `If-None-Match` of a method other than `GET` and `HEAD`,
 * are answered with a problem with status 412 (Precondition Failed), and a `GET` or `HEAD` whose representation did not
 * change with 304 (Not Modified) and the validators of [state]. A successful reply of [produce] gets the `ETag` and
 * `Last-Modified` of [state] unless it sets them itself.
 *
 * @receiver the request.
 * @param state the current representation of the resource.
 * @param produce answers the request when every precondition holds.
 * @return the reply.
 * @since 1.0.0
 */
public suspend fun Request.conditional(state: RepresentationState, produce: suspend () -> Reply): Reply =
    when (preconditions().evaluate(method, state)) {
        PreconditionDecision.Proceed -> {
            validated(produce(), state)
        }

        PreconditionDecision.NotModified -> {
            Reply(status = Statuses.NOT_MODIFIED, headers = validatorsOf(state))
        }

        PreconditionDecision.PreconditionFailed -> {
            context.problems.problem(status = Statuses.PRECONDITION_FAILED, code = ServerCodes.PRECONDITION_FAILED)
        }
    }

internal fun validatorsOf(state: RepresentationState): List<Pair<String, String>> = listOfNotNull(
    state.entityTag?.let { tag -> ReplyBuilder.ETAG to tag.toString() },
    state.lastModified?.let { date -> ReplyBuilder.LAST_MODIFIED to date.toString() },
)

internal fun joined(request: Request, name: String): String? {
    val values = request.headers.all(name)
    return values.joinToString(", ").takeIf { values.isNotEmpty() }
}

private fun validated(reply: Reply, state: RepresentationState): Reply {
    if (reply.status !in FIRST_SUCCESS..LAST_SUCCESS) return reply
    return validatorsOf(state).fold(reply) { current, (name, value) -> current.withDefaultHeader(name, value) }
}
