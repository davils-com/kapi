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

package com.davils.kapi.runtime.client

import com.davils.konfetti.core.Event
import com.davils.konfetti.core.TopicKey
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.time.Duration

/**
 * What an [ApiClient] reports on the topic [TOPIC] of its event bus: retries, redirects, refreshed credentials and
 * resumed streams.
 *
 * An event names the client and the operation, never a URL, a parameter or a credential. [toString] shows the names
 * through `SafeText`.
 *
 * @since 1.0.0
 */
public sealed interface ClientEvent : Event {
    /**
     * The name of the client.
     *
     * @since 1.0.0
     */
    public val api: String

    /**
     * The operation of the call.
     *
     * @since 1.0.0
     */
    public val operation: String

    /**
     * The resilience policy sends a call again.
     *
     * @property api the name of the client.
     * @property operation the operation of the call.
     * @property attempt the number of the attempt that starts, 2 for the first retry.
     * @since 1.0.0
     */
    public class Retrying internal constructor(
        override val api: String,
        override val operation: String,
        public val attempt: Int,
    ) : ClientEvent {
        override fun toString(): String =
            "Retrying(${SafeText.text(api)}, ${SafeText.text(operation)}, attempt=$attempt)"
    }

    /**
     * The client follows a redirect.
     *
     * @property api the name of the client.
     * @property operation the operation of the call.
     * @property status the status of the redirect.
     * @property hop the number of the hop, 1 for the first.
     * @property isCrossOrigin whether the hop leads to another origin, where the credentials do not follow.
     * @since 1.0.0
     */
    public class Redirected internal constructor(
        override val api: String,
        override val operation: String,
        public val status: Int,
        public val hop: Int,
        public val isCrossOrigin: Boolean,
    ) : ClientEvent {
        override fun toString(): String = "Redirected(${SafeText.text(api)}, ${SafeText.text(operation)}, " +
            "status=$status, hop=$hop, isCrossOrigin=$isCrossOrigin)"
    }

    /**
     * The client asked the source of [scheme] for a new credential after the server rejected one.
     *
     * @property api the name of the client.
     * @property operation the operation of the call.
     * @property scheme the name of the scheme.
     * @property isRefreshed whether the source has a new credential.
     * @since 1.0.0
     */
    public class Refreshed internal constructor(
        override val api: String,
        override val operation: String,
        public val scheme: String,
        public val isRefreshed: Boolean,
    ) : ClientEvent {
        override fun toString(): String = "Refreshed(${SafeText.text(api)}, ${SafeText.text(operation)}, " +
            "${SafeText.text(scheme)}, isRefreshed=$isRefreshed)"
    }

    /**
     * The client reconnects a stream of server-sent events after [delay].
     *
     * @property api the name of the client.
     * @property operation the operation of the call.
     * @property attempt the number of the reconnection, 1 for the first.
     * @property delay the wait before it.
     * @property isResumed whether it names the last event it saw in `Last-Event-ID`.
     * @since 1.0.0
     */
    public class Reconnecting internal constructor(
        override val api: String,
        override val operation: String,
        public val attempt: Int,
        public val delay: Duration,
        public val isResumed: Boolean,
    ) : ClientEvent {
        override fun toString(): String = "Reconnecting(${SafeText.text(api)}, ${SafeText.text(operation)}, " +
            "attempt=$attempt, delay=$delay, isResumed=$isResumed)"
    }

    /**
     * The topic of the events.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The topic `kapi.client`, declared on the bus when a client first reports to it.
         *
         * @since 1.0.0
         */
        public val TOPIC: TopicKey<ClientEvent> = TopicKey("kapi.client")
    }
}
