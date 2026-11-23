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

package com.davils.kapi.runtime.client.stream

import com.davils.kapi.runtime.client.ClientViolationCodes
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * How a stream of server-sent events reconnects, as the event source of the WHATWG HTML standard does.
 *
 * A stream reconnects when its connection broke or the server closed a stream it answered with `text/event-stream`,
 * never after `204 No Content`, another status or another media type. It waits the `retry` the server sent, else
 * [delay], at most [maximumDelay], and names the last event ID it saw in `Last-Event-ID` so the server resumes the
 * stream. After [maxReconnects] reconnections the stream ends. The waits run in the coroutine of the collector. A
 * reconnection is immutable and safe to share.
 *
 * @since 1.0.0
 */
public class Reconnection internal constructor(builder: ReconnectionBuilder) : Validatable {
    /**
     * The most reconnections of one stream.
     *
     * @since 1.0.0
     */
    public val maxReconnects: Int = builder.maxReconnects

    /**
     * The wait before a reconnection while the server named no `retry`.
     *
     * @since 1.0.0
     */
    public val delay: Duration = builder.delay

    /**
     * The longest wait.
     *
     * @since 1.0.0
     */
    public val maximumDelay: Duration = builder.maximumDelay

    override fun ValidationScope.validate() {
        if (maxReconnects !in 1..HARD_RECONNECTS) {
            fail(::maxReconnects, "must be from 1 to $HARD_RECONNECTS", ClientViolationCodes.LIMIT_INVALID)
        }
        if (delay.isNegative() || delay > maximumDelay) {
            fail(::delay, "must not be negative nor above the longest wait", ClientViolationCodes.TIMEOUT_INVALID)
        }
        if (maximumDelay > HARD_DELAY) {
            fail(::maximumDelay, "must be at most $HARD_DELAY", ClientViolationCodes.TIMEOUT_INVALID)
        }
    }

    override fun toString(): String =
        "Reconnection(maxReconnects=$maxReconnects, delay=$delay, maximumDelay=$maximumDelay)"

    /**
     * The hard ceilings of a reconnection.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The most reconnections of any stream.
         *
         * @since 1.0.0
         */
        public const val HARD_RECONNECTS: Int = 1_000

        /**
         * The longest wait of any stream: one hour.
         *
         * @since 1.0.0
         */
        public val HARD_DELAY: Duration = 1.hours
    }
}
