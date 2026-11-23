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

package com.davils.kapi.spec.asyncapi.compatibility

import com.davils.kapi.spec.asyncapi.AsyncApi
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.StructureCheck
import com.davils.kapi.spec.asyncapi.fixture.readable
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.core.compatibility.Change
import com.davils.kapi.spec.core.compatibility.ComparisonBuilder

private const val PREFIX: String = "asyncapi.compatibility."

internal val base: String = """
    |servers:
    |  broker:
    |    host: broker.example.com
    |    protocol: ws
    |channels:
    |  chat:
    |    address: chat
    |    messages:
    |      said:
    |        ${'$'}ref: '#/components/messages/Said'
    |      left:
    |        ${'$'}ref: '#/components/messages/Left'
    |  echo:
    |    address: echo
    |operations:
    |  say:
    |    action: send
    |    channel:
    |      ${'$'}ref: '#/channels/chat'
    |    messages:
    |      - ${'$'}ref: '#/channels/chat/messages/said'
    |components:
    |  messages:
    |    Said:
    |      contentType: application/json
    |      payload:
    |        type: object
    |        required: [text]
    |        properties:
    |          text: {type: string, maxLength: 100}
    |    Left:
    |      payload: {type: string}
""".trimMargin()

internal fun ruleOf(change: Change): String = change.code.value.removePrefix(PREFIX)

internal suspend fun documentOf(body: String): Document {
    val reading = readingOf(body) { structure = StructureCheck.Reader }
    return reading.readable()
}

internal suspend fun Pair<String, String>.changes(configure: ComparisonBuilder.() -> Unit = {}): List<Change> {
    val baseline = documentOf(first)
    val candidate = documentOf(second)
    val comparison = AsyncApi.compare(baseline = baseline, candidate = candidate, configure = configure)
    return comparison.changes
}

internal suspend fun Pair<String, String>.rules(configure: ComparisonBuilder.() -> Unit = {}): List<String> =
    changes(configure).map(::ruleOf)

internal fun String.edited(change: Pair<String, String>): String {
    check(change.first in this) { "The text does not hold ${change.first}" }
    return replace(oldValue = change.first, newValue = change.second)
}
