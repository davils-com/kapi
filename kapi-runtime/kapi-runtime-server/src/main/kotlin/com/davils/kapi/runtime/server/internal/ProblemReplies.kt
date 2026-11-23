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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.problem.Problem
import com.davils.kapi.runtime.core.problem.ProblemSerializer
import com.davils.kapi.runtime.core.problem.ProblemViolation
import com.davils.kapi.runtime.server.problem.ProblemSettings
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.ReplyBody
import kotlinx.io.bytestring.encodeToByteString

internal class ProblemReplies(private val codecs: Codecs, private val settings: ProblemSettings) {
    fun problem(
        status: Int,
        code: String,
        violations: List<ProblemViolation> = emptyList(),
        headers: List<Pair<String, String>> = emptyList(),
    ): Reply {
        val problem = Problem(
            type = settings.typeBase?.let { base -> base + code } ?: Problem.BLANK_TYPE,
            title = titleOf(status),
            status = status,
            code = code,
            violations = violations,
        )
        val text = codecs.json.encodeToString(ProblemSerializer, problem)
        val body = ReplyBody.Bytes(MediaType.ProblemJson, text.encodeToByteString())
        return Reply(status = status, headers = headers, body = body)
    }

    fun invalid(violations: List<ProblemViolation>): Reply =
        problem(status = Statuses.BAD_REQUEST, code = ServerCodes.INVALID, violations = violations)

    fun refused(admission: Admission.Refused): Reply {
        val status = Statuses.FORBIDDEN.takeIf { admission.isForbidden } ?: Statuses.UNAUTHORIZED
        val code = ServerCodes.FORBIDDEN.takeIf { admission.isForbidden } ?: ServerCodes.UNAUTHENTICATED
        val headers = admission.challenges.map { challenge -> WWW_AUTHENTICATE to challenge }
        return problem(status = status, code = code, headers = headers)
    }

    fun internal(): Reply = problem(status = Statuses.INTERNAL, code = ServerCodes.INTERNAL)

    private fun titleOf(status: Int): String = titles[status] ?: "The server could not answer"

    private companion object {
        const val WWW_AUTHENTICATE: String = "WWW-Authenticate"

        val titles: Map<Int, String> = mapOf(
            Statuses.BAD_REQUEST to "The request is invalid",
            Statuses.UNAUTHORIZED to "The request carries no valid credentials",
            Statuses.FORBIDDEN to "The caller may not do this",
            Statuses.NOT_ACCEPTABLE to "No media type the client accepts can be sent",
            Statuses.PRECONDITION_FAILED to "A precondition of the request failed",
            Statuses.TOO_LARGE to "The request is too large",
            Statuses.URI_TOO_LONG to "The URI of the request is too long",
            Statuses.UNSUPPORTED_MEDIA_TYPE to "The content of the request is not supported",
            Statuses.RANGE_NOT_SATISFIABLE to "No range of the request can be sent",
            Statuses.UPGRADE_REQUIRED to "The request must upgrade its protocol",
            Statuses.HEADERS_TOO_LARGE to "The header fields of the request are too large",
        )
    }
}
