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

package com.davils.kapi.runtime.server.fixture

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.response.headersOf
import com.davils.kapi.runtime.server.ServerLimits
import com.davils.kapi.runtime.server.internal.BodySource
import com.davils.kapi.runtime.server.internal.ProblemReplies
import com.davils.kapi.runtime.server.internal.RequestLine
import com.davils.kapi.runtime.server.internal.RequestState
import com.davils.kapi.runtime.server.internal.ServerContext
import com.davils.kapi.runtime.server.problem.ProblemSettings
import com.davils.kapi.runtime.server.request.Peer
import com.davils.kapi.runtime.server.request.Request
import kotlinx.io.bytestring.ByteString

internal fun requestOf(
    headers: Map<String, List<String>> = emptyMap(),
    method: String = "POST",
    path: String = PROBE,
    rawQuery: String? = null,
    content: ByteString = ByteString(),
): Request = Request(
    line = RequestLine(
        operation = "probe",
        method = method,
        scheme = "https",
        authority = "example.com",
        path = path,
        rawQuery = rawQuery,
    ),
    pathParameters = emptyMap(),
    query = emptyList(),
    headers = headersOf(headers),
    peer = Peer(),
    principal = null,
    body = BodySource.buffered(content, ServerLimits.Default),
    context = ServerContext(
        codecs = Codecs.Default,
        limits = ServerLimits.Default,
        problems = ProblemReplies(Codecs.Default, ProblemSettings.Default),
        clock = fixedClock,
    ),
    state = RequestState(),
)
