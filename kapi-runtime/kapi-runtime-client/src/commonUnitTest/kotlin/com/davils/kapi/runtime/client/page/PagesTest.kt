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

package com.davils.kapi.runtime.client.page

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

private const val STUCK: Int = 3

private val cursors: Map<String?, List<String>> =
    mapOf(null to listOf("a", "b"), "b" to listOf("c"), "c" to emptyList())

private val offsets: Map<Long, List<String>> = mapOf(0L to listOf("a", "b"), 2L to listOf("c"), 3L to emptyList())

private val numbered: Map<Long, List<String>> = mapOf(1L to listOf("a", "b"), 2L to listOf("c"), 3L to emptyList())

class PagesTest : FunSpec({
    context("pages that follow a cursor") {
        test("are every page up to the one that names no next") {
            val asked = mutableListOf<String?>()
            val pages = cursorPages(next = { page: List<String> -> page.lastOrNull() }) { cursor ->
                asked += cursor
                cursors.getValue(cursor)
            }

            pages.toList() shouldBe listOf(listOf("a", "b"), listOf("c"), emptyList())
            asked shouldBe listOf(null, "b", "c")
        }

        test("end when a page names the cursor it was asked with") {
            cursorPages(next = { _: String -> "same" }) { cursor -> cursor ?: "first" }.toList() shouldBe
                listOf("first", "same")
        }

        test("are asked for one at a time, so a caller can stop") {
            val asked = mutableListOf<String?>()
            val pages = cursorPages(next = { page: String -> page + "x" }) { cursor ->
                asked += cursor
                cursor.orEmpty()
            }

            pages.take(STUCK).toList() shouldBe listOf("", "x", "xx")
            asked shouldBe listOf(null, "x", "xx")
        }
    }

    context("pages that follow an offset") {
        test("start at zero, move by what a page held and end with an empty page or no page") {
            val asked = mutableListOf<Long>()
            val pages = offsetPages(size = { page: List<String> -> page.size }) { offset ->
                asked += offset
                offsets.getValue(offset)
            }

            pages.toList() shouldBe listOf(listOf("a", "b"), listOf("c"), emptyList())
            asked shouldBe offsets.keys.toList()
            offsetPages(size = { _: String -> null }) { offset -> "failed at $offset" }.toList() shouldBe
                listOf("failed at 0")
        }
    }

    context("pages that follow a number") {
        test("start at one, move by one and end with an empty page or no page") {
            val asked = mutableListOf<Long>()
            val pages = numberedPages(size = { page: List<String> -> page.size }) { number ->
                asked += number
                numbered.getValue(number)
            }

            pages.toList() shouldBe listOf(listOf("a", "b"), listOf("c"), emptyList())
            asked shouldBe numbered.keys.toList()
            numberedPages(size = { _: String -> null }) { number -> "failed at $number" }.toList() shouldBe
                listOf("failed at 1")
        }
    }
})
