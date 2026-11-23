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

package com.davils.kapi.spec.asyncapi.testing

private const val LINE_END: String = "\n"

private const val WIDTH: Int = 9

private val blocks: List<String> = listOf("Aa", "BB")

private val messagesHead: List<String> = listOf("components:", "  messages:")

private val head: List<String> = listOf("asyncapi: 3.0.0", "info:", "  title: Hostile", "  version: 1.0.0")

internal object HostileDocuments {
    fun aliasBomb(levels: Int): String {
        val first = "x-level0: &level0 [${List(WIDTH) { "boom" }.joinToString()}]"
        val next = (1..levels).map { level ->
            val previous = "*level${level - 1}"
            "x-level$level: &level$level [${List(WIDTH) { previous }.joinToString()}]"
        }
        return document(listOf(first) + next)
    }

    fun deepNesting(depth: Int): String = document(listOf("x-deep: " + "[".repeat(depth) + "]".repeat(depth)))

    fun deepSchema(depth: Int): String {
        val nested = (0 until depth).map { level -> "  ".repeat(level + 3) + "items:" }
        val end = "  ".repeat(depth + 3) + "type: string"
        return document(listOf("components:", "  schemas:", "    Deep:") + nested + end)
    }

    fun unknownFields(count: Int): String = document((1..count).map { index -> "unknown$index: $index" })

    fun messageChain(length: Int): String {
        val links = (1..length).map { index -> "    M$index: {\$ref: '#/components/messages/M${index + 1}'}" }
        val end = "    M${length + 1}: {name: end}"
        return document(messagesHead + links + end)
    }

    fun manyChannels(count: Int): String {
        val channels = (1..count).flatMap { index ->
            listOf("  c$index:", "    address: 'c$index.{id}'", "    parameters:", "      id: {}")
        }
        val operations = (1..count).flatMap { index ->
            listOf("  o$index:", "    action: send", "    channel: {\$ref: '#/channels/c$index'}")
        }
        return document(listOf("channels:") + channels + "operations:" + operations)
    }

    fun collidingKeys(count: Int): List<String> {
        var keys = listOf("")
        while (keys.size < count) keys = keys.flatMap { key -> blocks.map { block -> key + block } }
        return keys.take(count)
    }

    fun collidingChannels(count: Int): String {
        val keys = collidingKeys(count)
        val channels = keys.map { key -> "  $key: {address: $key}" }
        val messages = keys.map { key -> "    $key: {name: $key}" }
        val extensions = keys.map { key -> "x-$key: $key" }
        return document(extensions + listOf("channels:") + channels + messagesHead + messages)
    }

    fun longKey(length: Int): String = document(listOf("? x" + "y".repeat(length), ": 1"))

    private fun document(lines: List<String>): String =
        (head + lines).joinToString(separator = LINE_END, postfix = LINE_END)
}
