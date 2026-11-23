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

package com.davils.kapi.runtime.server.reply

import com.davils.kapi.runtime.server.internal.HeaderChecks

private const val FIRST_STATUS: Int = 100

private const val LAST_STATUS: Int = 599

/**
 * What a handler answers a request with: a status, header fields and a body.
 *
 * Every header field has a token as its name and a value without control characters, so no reply can inject a field
 * or split the response; the fields Ktor writes itself, such as `Content-Type`, `Content-Length` and
 * `Transfer-Encoding`, are refused, since the body sets them. A reply is immutable; [toString] shows its status and the
 * kind of its body, never its content.
 *
 * @property status the status code, from 100 to 599.
 * @param headers the header fields in order; a name may repeat.
 * @property body the body.
 * @throws IllegalArgumentException if [status] lies outside 100 to 599 or a header field is malformed or reserved.
 * @since 1.0.0
 */
public class Reply(
    public val status: Int,
    headers: List<Pair<String, String>> = emptyList(),
    public val body: ReplyBody = ReplyBody.Empty,
) {
    /**
     * The header fields in order.
     *
     * @since 1.0.0
     */
    public val headers: List<Pair<String, String>> = headers.toList()

    private val parts: List<Any> get() = listOf(status, headers, body)

    init {
        require(status in FIRST_STATUS..LAST_STATUS) { "A status lies between 100 and 599, but was $status" }
        headers.forEach { (name, value) -> HeaderChecks.requireField(name, value) }
    }

    /**
     * Returns the first value of the header field [name].
     *
     * @param name the name, compared without case.
     * @return the value, or `null` if the reply has no such field.
     * @since 1.0.0
     */
    public fun header(name: String): String? =
        headers.firstOrNull { (candidate, _) -> candidate.equals(name, ignoreCase = true) }?.second

    /**
     * Returns this reply with the header field [name] added unless it has one already.
     *
     * @param name the name of the field.
     * @param value the value of the field.
     * @return the reply with the field.
     * @throws IllegalArgumentException if the field is malformed or reserved.
     * @since 1.0.0
     */
    public fun withDefaultHeader(name: String, value: String): Reply {
        if (header(name) != null) return this
        return Reply(status = status, headers = headers + (name to value), body = body)
    }

    override fun equals(other: Any?): Boolean = other is Reply && parts == other.parts

    override fun hashCode(): Int = parts.hashCode()

    override fun toString(): String = "Reply($status, $body)"
}
