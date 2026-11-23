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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.client.call

import com.davils.kapi.runtime.client.internal.Granted
import com.davils.kapi.runtime.core.header.IdempotencyKey
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private val idempotentMethods: Set<String> = setOf("GET", "HEAD", "OPTIONS", "TRACE", "PUT", "DELETE")

/**
 * One call of an operation, built by [apiCall]: its method, path, parameters, the media types it accepts, its content,
 * its security requirement and what it allows itself.
 *
 * A call is immutable and safe to share between threads; [change] makes a changed copy, such as in an interceptor.
 * [toString] names the operation and the method, never a parameter or the content, which may be secret.
 *
 * @since 1.0.0
 */
public class Call internal constructor(builder: CallBuilder) {
    /**
     * The `operationId` of the operation, which names the call in telemetry.
     *
     * @since 1.0.0
     */
    public val operation: String = builder.operation

    /**
     * The method in upper case, such as `GET`.
     *
     * @since 1.0.0
     */
    public val method: String = builder.method

    /**
     * The path below the base URL of the client, with every parameter already encoded.
     *
     * @since 1.0.0
     */
    public val path: String = builder.pathText

    /**
     * The query parameters in their order, not yet percent-encoded.
     *
     * @since 1.0.0
     */
    public val query: List<ParameterEntry> = builder.queryEntries.toList()

    /**
     * The whole query of a parameter `in: querystring`, already percent-encoded, which comes before [query], or
     * `null`.
     *
     * @since 1.0.0
     */
    public val queryString: String? = builder.rawQuery

    /**
     * The header fields in their order.
     *
     * @since 1.0.0
     */
    public val headers: List<ParameterEntry> = builder.headerEntries.toList()

    /**
     * The cookies in their order.
     *
     * @since 1.0.0
     */
    public val cookies: List<ParameterEntry> = builder.cookieEntries.toList()

    /**
     * The media ranges the call accepts in an answer, written as one `Accept` field.
     *
     * @since 1.0.0
     */
    public val accept: List<MediaRange> = builder.accepted.toList()

    /**
     * The content held in memory, or `null`.
     *
     * @since 1.0.0
     */
    public val body: Body? = builder.content

    /**
     * The alternatives of the security requirement in their order; empty for an operation open to anyone.
     *
     * @since 1.0.0
     */
    public val security: List<List<SecurityUse>> = builder.alternatives.toList()

    /**
     * Why the call must not be sent, found while it was built, or `null`.
     *
     * @since 1.0.0
     */
    public val refusal: CallFailure? = builder.refusal

    /**
     * The time the call allows itself, or `null` for that of the client.
     *
     * @since 1.0.0
     */
    public val timeout: Timeout? = builder.limits

    /**
     * Whether the call may be sent again with the same effect: its method is idempotent by RFC 9110 §9.2.2, or the
     * document declares the operation idempotent.
     *
     * @since 1.0.0
     */
    public val isIdempotent: Boolean = builder.isMarkedIdempotent || method in idempotentMethods

    /**
     * The key of the `Idempotency-Key` field, which lets a server recognize a repeated call, or `null`.
     *
     * @since 1.0.0
     */
    public val idempotencyKey: IdempotencyKey? = builder.key

    /**
     * The absolute URL the call goes to instead of the base URL and the path, such as a webhook or a link, or `null`.
     *
     * @since 1.0.0
     */
    public val destination: String? = builder.target

    /**
     * The content streamed from a flow, or `null`.
     *
     * @since 1.0.0
     */
    public val upload: Upload? = builder.streamed

    /**
     * The protocol the call switches to, or `null`.
     *
     * @since 1.0.0
     */
    public val upgrade: Upgrade? = builder.switched

    /**
     * Whether the call can be sent again: it carries neither an [upload] nor an [upgrade].
     *
     * @since 1.0.0
     */
    public val isReplayable: Boolean get() = upload == null && upgrade == null

    /**
     * Whether a resilience policy may retry the call: it is replayable and either [isIdempotent] or carries an
     * [idempotencyKey].
     *
     * @since 1.0.0
     */
    public val isRetryable: Boolean get() = isReplayable && (isIdempotent || idempotencyKey != null)

    private val parts: List<Any?>
        get() = listOf(
            operation,
            method,
            path,
            query,
            queryString,
            headers,
            cookies,
            accept,
            body,
            security,
            refusal,
            timeout,
            isIdempotent,
            idempotencyKey,
            destination,
            upload,
            upgrade,
        )

    /**
     * Returns a copy of this call changed by [configure], which adds to what the call already holds.
     *
     * @param configure changes the copy.
     * @return the changed copy.
     * @since 1.0.0
     */
    public fun change(configure: CallBuilder.() -> Unit): Call {
        val builder = CallBuilder.from(this)
        builder.configure()
        return builder.build()
    }

    internal fun withCredentials(granted: Granted): Call {
        val builder = CallBuilder.from(this)
        builder.headerEntries += granted.headers
        builder.queryEntries += granted.query
        builder.cookieEntries += granted.cookies
        return builder.build()
    }

    internal fun withoutQuery(target: String): Call {
        val builder = CallBuilder.from(this)
        builder.clearQuery()
        builder.destination(target)
        return builder.build()
    }

    override fun equals(other: Any?): Boolean = other is Call && parts == other.parts

    override fun hashCode(): Int = parts.hashCode()

    override fun toString(): String = "Call(${SafeText.text(operation)}, ${SafeText.text(method)})"
}
