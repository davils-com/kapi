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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configures a [Reconnection]; by default it reconnects five times, three seconds apart unless the server asks for
 * another time, and never waits longer than a minute.
 *
 * A builder is not thread-safe; the reconnection it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ReconnectionBuilder internal constructor() : ValidatingBuilder<Reconnection>() {
    override val subject: String get() = "reconnection"

    /**
     * The most reconnections of one stream, from 1 to [Reconnection.HARD_RECONNECTS].
     *
     * @since 1.0.0
     */
    public var maxReconnects: Int = DEFAULT_RECONNECTS
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The wait before a reconnection while the server named no `retry`.
     *
     * @since 1.0.0
     */
    public var delay: Duration = 3.seconds
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The longest wait, whatever the server asks for in `retry`.
     *
     * @since 1.0.0
     */
    public var maximumDelay: Duration = 1.minutes
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): Reconnection = Reconnection(this)

    private companion object {
        const val DEFAULT_RECONNECTS: Int = 5
    }
}

/**
 * Builds how a stream of server-sent events reconnects.
 *
 * @param configure changes the reconnection from its defaults.
 * @return the reconnection.
 * @throws com.davils.kore.core.validation.report.ValidationException if a limit is out of its bounds.
 * @since 1.0.0
 */
public fun reconnection(configure: ReconnectionBuilder.() -> Unit = {}): Reconnection {
    val builder = ReconnectionBuilder()
    builder.configure()
    return builder.build()
}
