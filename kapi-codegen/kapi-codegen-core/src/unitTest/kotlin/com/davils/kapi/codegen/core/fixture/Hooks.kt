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

package com.davils.kapi.codegen.core.fixture

import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.options.Artifact
import io.kotest.matchers.types.shouldBeInstanceOf

internal const val WEBHOOKS_SENDER: String = "PetstoreWebhooksSender"
internal const val WEBHOOKS_RECEIVER: String = "PetstoreWebhooksReceiver"
internal const val WEBHOOKS_ROUTES: String = "PetstoreWebhooksRoutes"
internal const val CALLBACKS_SENDER: String = "PetstoreCallbacksSender"
internal const val TAKEN: String = "responses: {'204': {description: taken}}"

internal fun webhook(key: String, vararg fields: String): List<String> {
    val all = fields.toList() + TAKEN
    return listOf("webhooks:", "  $key: {post: {${all.joinToString()}}}")
}

internal fun callback(vararg fields: String, expressions: String = "'{\$request.body#/url}'"): List<String> {
    val all = fields.toList() + TAKEN
    val items = expressions.split(" ").joinToString { expression -> "$expression: {post: {${all.joinToString()}}}" }
    return paths("/subscriptions: {post: {operationId: subscribe, $OK, callbacks: {petChanged: {$items}}}}")
}

internal suspend fun hookGenerationOf(
    lines: List<String>,
    artifacts: Set<Artifact> = setOf(
        Artifact.Models,
        Artifact.Senders,
        Artifact.Receivers,
    ),
): Generation {
    val reading = read(lines)
    val generator = generator { this.artifacts = artifacts }
    return generator.generate(reading.document, reading.sources)
}

internal suspend fun hookFilesOf(lines: List<String>, artifacts: Set<Artifact>): List<String> {
    val generated = hookGenerationOf(lines, artifacts).shouldBeInstanceOf<Generation.Generated>()
    return generated.files.map { file -> file.path.substringAfter("petstore/") }
}

internal suspend fun hookFileOf(lines: List<String>, name: String): String {
    val generated = hookGenerationOf(lines).shouldBeInstanceOf<Generation.Generated>()
    return generated.files.first { file -> file.path.endsWith("/$name.kt") }.content
}
