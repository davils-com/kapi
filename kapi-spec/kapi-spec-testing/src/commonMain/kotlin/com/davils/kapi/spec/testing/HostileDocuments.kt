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

private val head: List<String> = listOf("openapi: 3.1.0", "info:", "  title: Hostile", "  version: 1.0.0", "paths: {}")

/**
 * OpenAPI documents built to probe the limits of a reader: alias bombs, deep nesting, many unknown fields and long
 * chains of references, each sized by the caller.
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

    private fun document(lines: List<String>): String =
        (head + lines).joinToString(separator = LINE_END, postfix = LINE_END)
}
