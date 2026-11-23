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

package com.davils.kapi.spec.testing

private const val LINE_END: String = "\n"

private const val WIDTH: Int = 9

private val blocks: List<String> = listOf("Aa", "BB")

private val head: List<String> = listOf("openapi: 3.1.0", "info:", "  title: Hostile", "  version: 1.0.0", "paths: {}")

/**
 * OpenAPI documents built to probe the limits of a reader: alias bombs, deep nesting, many unknown fields, long
 * chains of references and keys whose hash codes collide, each sized by the caller.
 *
 * @since 1.0.0
 */
public object HostileDocuments {
    /**
     * Returns a document whose aliases expand to nine to the power of [levels] nodes.
     *
     * @param levels the levels of aliases.
     * @return the YAML text.
     * @since 1.0.0
     */
    public fun aliasBomb(levels: Int): String {
        val first = "x-level0: &level0 [${List(WIDTH) { "boom" }.joinToString()}]"
        val next = (1..levels).map { level ->
            val previous = "*level${level - 1}"
            "x-level$level: &level$level [${List(WIDTH) { previous }.joinToString()}]"
        }
        return document(listOf(first) + next)
    }

    /**
     * Returns a document with an extension of sequences nested [depth] deep.
     *
     * @param depth the depth of the nesting.
     * @return the YAML text.
     * @since 1.0.0
     */
    public fun deepNesting(depth: Int): String = document(listOf("x-deep: " + "[".repeat(depth) + "]".repeat(depth)))

    /**
     * Returns a document with [count] fields the specification does not know.
     *
     * @param count the number of fields.
     * @return the YAML text.
     * @since 1.0.0
     */
    public fun unknownFields(count: Int): String = document((1..count).map { index -> "unknown$index: $index" })

    /**
     * Returns a document with a chain of [length] responses that each refer to the next.
     *
     * @param length the length of the chain.
     * @return the YAML text.
     * @since 1.0.0
     */
    public fun referenceChain(length: Int): String {
        val links = (1..length).map { index -> "    R$index: {\$ref: '#/components/responses/R${index + 1}'}" }
        val end = "    R${length + 1}: {description: The end}"
        return document(listOf("components:", "  responses:") + links + end)
    }

    /**
     * Returns [count] distinct keys that all share one hash code, such as `AaAa` and `BBBB`, the keys an attacker
     * chooses to turn a hash map without tree bins, like those of Wasm, into a list.
     *
     * @param count the number of keys.
     * @return the keys, each of the same length.
     * @since 1.0.0
     */
    public fun collidingKeys(count: Int): List<String> {
        var keys = listOf("")
        while (keys.size < count) keys = keys.flatMap { key -> blocks.map { block -> key + block } }
        return keys.take(count)
    }

    /**
     * Returns a document whose schemas of the components and whose properties, extensions and enum of one of them are
     * named by [count] keys of [collidingKeys].
     *
     * @param count the number of keys.
     * @return the YAML text.
     * @since 1.0.0
     */
    public fun collidingKeysDocument(count: Int): String {
        val keys = collidingKeys(count)
        val schemas = keys.map { key -> "    $key: {type: string}" }
        val properties = keys.map { key -> "        $key: {type: integer}" }
        val extensions = keys.map { key -> "      x-$key: $key" }
        val values = keys.joinToString(prefix = "[", postfix = "]")
        val colliding = listOf("    Colliding:", "      type: object", "      enum: $values", "      properties:")
        return document(listOf("components:", "  schemas:") + schemas + colliding + properties + extensions)
    }

    private fun document(lines: List<String>): String =
        (head + lines).joinToString(separator = LINE_END, postfix = LINE_END)
}
