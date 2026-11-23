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

import com.davils.kapi.runtime.server.ApiServer
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.request.Request
import com.davils.kapi.runtime.server.webhook.DeliveryVerifier
import com.davils.kapi.runtime.server.webhook.Verdict
import com.davils.kore.core.failure.attempted
import kotlinx.io.bytestring.ByteString

private const val FAILED: String = "The verifier failed"

internal object Deliveries {
    suspend fun verify(
        server: ApiServer,
        operation: Operation,
        request: Request,
        next: suspend (Request) -> Reply,
    ): Reply {
        val context = server.context
        val verifier = server.settings.verifier
        if (verifier == null && operation.security.isEmpty()) return unverified(server)
        return when (val read = request.body.bytes(context.limits.maxRequestBytes)) {
            is BodyRead.Refused -> {
                context.problems.problem(read.refusal.status, read.refusal.code)
            }

            is BodyRead.Read -> {
                val buffered = request.buffered(read.value)
                proceed(
                    server = server,
                    verdict = verdictOf(verifier, buffered, read.value),
                    request = buffered,
                    next = next,
                )
            }
        }
    }

    private suspend fun verdictOf(verifier: DeliveryVerifier?, request: Request, content: ByteString): Verdict {
        val found = verifier ?: return Verdict.Accepted
        return attempted { found.verify(request, content) }.getOrNull() ?: Verdict.Refused(FAILED)
    }

    private suspend fun proceed(
        server: ApiServer,
        verdict: Verdict,
        request: Request,
        next: suspend (Request) -> Reply,
    ): Reply {
        if (verdict != Verdict.Accepted) return unverified(server)
        return next(request)
    }

    private fun unverified(server: ApiServer): Reply =
        server.context.problems.problem(Statuses.UNAUTHORIZED, ServerCodes.UNVERIFIED)
}
