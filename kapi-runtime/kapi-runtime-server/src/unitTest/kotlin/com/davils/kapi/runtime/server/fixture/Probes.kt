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

import com.davils.kapi.runtime.server.ServerSettingsBuilder
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.request.Input
import io.ktor.server.testing.ApplicationTestBuilder

private const val NOTHING: String = "null"

internal const val PROBE: String = "/probe"

internal fun probe(path: String = PROBE, method: String = "POST", read: suspend (Input) -> Any?): Operation =
    Operation(name = "probe", method = method, path = path) { request ->
        val input = request.input()
        val value = read(input)
        input.refused().takeUnless { input.isValid } ?: reply(OK) { text(value?.toString() ?: NOTHING) }
    }

internal suspend fun probing(
    operation: Operation,
    settings: ServerSettingsBuilder.() -> Unit = {},
    block: suspend ApplicationTestBuilder.() -> Unit,
) {
    serve(settings = settings, routes = { operation(operation) }, block = block)
}
