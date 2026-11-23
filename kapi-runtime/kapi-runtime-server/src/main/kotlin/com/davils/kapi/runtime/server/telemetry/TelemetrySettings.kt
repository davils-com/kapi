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

package com.davils.kapi.runtime.server.telemetry

import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.trace.Tracer
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * Where a server reports: the spans of its tracer, the meters of a registry and the events of an event bus.
 *
 * Every request of an operation runs in a span `kapi.server.request` of kind server with the attributes
 * `kapi.operation`, `http.request.method` and `http.response.status_code`. The registry, when given, counts the
 * requests in `kapi.server.requests` by operation and outcome (`success`, `client_error`, `server_error`), times them
 * in `kapi.server.request.duration` by operation and counts the sessions of channels in `kapi.server.channel.sessions`
 * by channel. The event bus, when given, carries every [ServerEvent] on [ServerTopics.EVENTS]; an event is built only
 * when the topic has a consumer, and one the bus refuses is dropped, never waited for. kapi logs nothing. The settings
 * are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class TelemetrySettings internal constructor(builder: TelemetrySettingsBuilder) : Validatable {
    /**
     * The tracer of the spans; [Tracer.None] by default.
     *
     * @since 1.0.0
     */
    public val tracer: Tracer = builder.tracer

    /**
     * The registry of the meters, or `null` for none.
     *
     * @since 1.0.0
     */
    public val meters: MeterRegistry? = builder.registry

    /**
     * The event bus of the events, or `null` for none.
     *
     * @since 1.0.0
     */
    public val events: EventBus? = builder.bus

    override fun ValidationScope.validate(): Unit = Unit

    override fun toString(): String = "TelemetrySettings(meters=${meters != null}, events=${events != null})"

    /**
     * The default settings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The settings that trace, count and publish nothing.
         *
         * @since 1.0.0
         */
        public val None: TelemetrySettings = TelemetrySettingsBuilder().build()
    }
}
