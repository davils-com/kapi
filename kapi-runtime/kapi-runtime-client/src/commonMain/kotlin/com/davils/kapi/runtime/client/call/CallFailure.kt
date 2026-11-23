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

import com.davils.kapi.runtime.core.destination.DestinationRefusal
import com.davils.kapi.runtime.core.framing.FramingFailure
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * Why a call got no answer of the server.
 *
 * A failure never carries an address, a credential or the content of a request; [toString] shows the text it carries
 * through `SafeText`.
 *
 * @since 1.0.0
 */
public sealed interface CallFailure {
    /**
     * The server could not be reached, such as when the connection was refused or broke.
     *
     * @property cause the name of the type of the failure, without its message, which may name the address.
     * @since 1.0.0
     */
    public class Unreachable(public val cause: String) : CallFailure {
        override fun equals(other: Any?): Boolean = other is Unreachable && cause == other.cause

        override fun hashCode(): Int = cause.hashCode()

        override fun toString(): String = "Unreachable(${SafeText.text(cause)})"
    }

    /**
     * The server did not answer within the time the call allows.
     *
     * @since 1.0.0
     */
    public object TimedOut : CallFailure {
        override fun toString(): String = "TimedOut"
    }

    /**
     * The answer, a line or a record of it is larger than the client reads.
     *
     * @property limit the limit in bytes.
     * @since 1.0.0
     */
    public class TooLarge(public val limit: Long) : CallFailure {
        override fun equals(other: Any?): Boolean = other is TooLarge && limit == other.limit

        override fun hashCode(): Int = limit.hashCode()

        override fun toString(): String = "TooLarge($limit)"
    }

    /**
     * No alternative of the security requirement can be met, or a source has no credential.
     *
     * @since 1.0.0
     */
    public object CredentialMissing : CallFailure {
        override fun toString(): String = "CredentialMissing"
    }

    /**
     * The source of [scheme] handed out a credential that could inject a header field; it was not sent.
     *
     * @property scheme the name of the scheme.
     * @since 1.0.0
     */
    public class CredentialMalformed(public val scheme: String) : CallFailure {
        override fun equals(other: Any?): Boolean = other is CredentialMalformed && scheme == other.scheme

        override fun hashCode(): Int = scheme.hashCode()

        override fun toString(): String = "CredentialMalformed(${SafeText.text(scheme)})"
    }

    /**
     * A value of the path parameter [parameter] would change the path, such as `..`; the call was not sent.
     *
     * @property parameter the name of the parameter.
     * @since 1.0.0
     */
    public class PathRefused(public val parameter: String) : CallFailure {
        override fun equals(other: Any?): Boolean = other is PathRefused && parameter == other.parameter

        override fun hashCode(): Int = parameter.hashCode()

        override fun toString(): String = "PathRefused(${SafeText.text(parameter)})"
    }

    /**
     * A header field, a cookie, a form field or a media type of the call cannot be written safely, such as a value
     * with a line break; the call was not sent.
     *
     * @property parameter the name of the parameter or field.
     * @since 1.0.0
     */
    public class ParameterRefused(public val parameter: String) : CallFailure {
        override fun equals(other: Any?): Boolean = other is ParameterRefused && parameter == other.parameter

        override fun hashCode(): Int = parameter.hashCode()

        override fun toString(): String = "ParameterRefused(${SafeText.text(parameter)})"
    }

    /**
     * The interceptor [interceptor] failed; the call ended there.
     *
     * @property interceptor the name of the interceptor.
     * @since 1.0.0
     */
    public class Aborted(public val interceptor: String) : CallFailure {
        override fun equals(other: Any?): Boolean = other is Aborted && interceptor == other.interceptor

        override fun hashCode(): Int = interceptor.hashCode()

        override fun toString(): String = "Aborted(${SafeText.text(interceptor)})"
    }

    /**
     * The resilience policy of the client did not admit the call, such as an open circuit breaker or a full bulkhead.
     *
     * @property reason the kind of the component that refused, such as `circuit-breaker`.
     * @since 1.0.0
     */
    public class Rejected(public val reason: String) : CallFailure {
        override fun equals(other: Any?): Boolean = other is Rejected && reason == other.reason

        override fun hashCode(): Int = reason.hashCode()

        override fun toString(): String = "Rejected(${SafeText.text(reason)})"
    }

    /**
     * The call names no destination and the client has no base URL.
     *
     * @since 1.0.0
     */
    public object BaseUrlMissing : CallFailure {
        override fun toString(): String = "BaseUrlMissing"
    }

    /**
     * The destination policy refused the URL of the call, of a redirect hop, of a page or of a channel.
     *
     * @property refusal why.
     * @since 1.0.0
     */
    public class DestinationRefused(public val refusal: DestinationRefusal) : CallFailure {
        override fun equals(other: Any?): Boolean = other is DestinationRefused && refusal == other.refusal

        override fun hashCode(): Int = refusal.hashCode()

        override fun toString(): String = "DestinationRefused($refusal)"
    }

    /**
     * The server redirected more often than the redirect policy of the client follows.
     *
     * @property limit the most hops the client follows.
     * @since 1.0.0
     */
    public class TooManyRedirects(public val limit: Int) : CallFailure {
        override fun equals(other: Any?): Boolean = other is TooManyRedirects && limit == other.limit

        override fun hashCode(): Int = limit

        override fun toString(): String = "TooManyRedirects($limit)"
    }

    /**
     * A link of the server leads to another origin than the call it belongs to, where the credentials of the call
     * must not follow.
     *
     * @since 1.0.0
     */
    public object CrossOrigin : CallFailure {
        override fun toString(): String = "CrossOrigin"
    }

    /**
     * A stream broke its framing, such as a record longer than the limit or text that is no UTF-8.
     *
     * @property failure what broke.
     * @since 1.0.0
     */
    public class Framing(public val failure: FramingFailure) : CallFailure {
        override fun equals(other: Any?): Boolean = other is Framing && failure == other.failure

        override fun hashCode(): Int = failure.hashCode()

        override fun toString(): String = "Framing($failure)"
    }

    /**
     * The `HttpClient` of the application has no `WebSockets` plugin, so a channel cannot be opened.
     *
     * @since 1.0.0
     */
    public object ChannelsNotInstalled : CallFailure {
        override fun toString(): String = "ChannelsNotInstalled"
    }

    /**
     * The server refused to open a channel and answered [status] instead of `101 Switching Protocols`.
     *
     * @property status the status of the answer, or 0 if the engine named none.
     * @since 1.0.0
     */
    public class HandshakeRefused(public val status: Int) : CallFailure {
        override fun equals(other: Any?): Boolean = other is HandshakeRefused && status == other.status

        override fun hashCode(): Int = status

        override fun toString(): String = "HandshakeRefused($status)"
    }
}
