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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Pages through an operation that names the cursor of the next page in each page.
 *
 * The first page is asked for without a cursor. The flow is cold and asks for a page only when the collector wants
 * the next one, so `take` bounds it; it ends after the page that names no cursor or the cursor it was asked with.
 *
 * @param R the type of a page.
 * @param next the cursor of the page after a page, or `null` on the last one.
 * @param call asks for the page of a cursor, `null` for the first.
 * @return the pages in their order.
 * @since 1.0.0
 */
public fun <R> cursorPages(next: (R) -> String?, call: suspend (String?) -> R): Flow<R> = flow {
    var cursor: String? = null
    var isOpen = true
    while (isOpen) {
        val page = call(cursor)
        emit(page)
        val following = next(page)
        isOpen = following != null && following != cursor
        cursor = following
    }
}

/**
 * Pages through an operation that skips the items of the pages before by an offset, starting at 0.
 *
 * Each page moves the offset by the items it held; the flow ends after a page that held none or is no page, such
 * as an error. The flow is cold and asks for a page only when the collector wants the next one.
 *
 * @param R the type of a page.
 * @param size the number of items of a page, or `null` for an answer that is no page.
 * @param call asks for the page at an offset.
 * @return the pages in their order.
 * @since 1.0.0
 */
public fun <R> offsetPages(size: (R) -> Int?, call: suspend (Long) -> R): Flow<R> =
    stepped(start = 0, call = call) { offset, page -> offset + (size(page) ?: 0) }

/**
 * Pages through an operation that numbers its pages, starting at 1.
 *
 * The flow ends after a page that held no item or is no page. The flow is cold and asks for a page only when the
 * collector wants the next one.
 *
 * @param R the type of a page.
 * @param size the number of items of a page, or `null` for an answer that is no page.
 * @param call asks for the page of a number.
 * @return the pages in their order.
 * @since 1.0.0
 */
public fun <R> numberedPages(size: (R) -> Int?, call: suspend (Long) -> R): Flow<R> =
    stepped(start = 1, call = call) { number, page ->
        number + (size(page) ?: 0).coerceIn(minimumValue = 0, maximumValue = 1)
    }

private fun <R> stepped(start: Long, call: suspend (Long) -> R, next: (Long, R) -> Long): Flow<R> = flow {
    var position = start
    var isOpen = true
    while (isOpen) {
        val page = call(position)
        emit(page)
        val following = next(position, page)
        isOpen = following != position
        position = following
    }
}
