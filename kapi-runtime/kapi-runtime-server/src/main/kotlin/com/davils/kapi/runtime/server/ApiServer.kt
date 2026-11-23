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

package com.davils.kapi.runtime.server

import com.davils.kapi.runtime.server.internal.Gate
import com.davils.kapi.runtime.server.internal.ProblemReplies
import com.davils.kapi.runtime.server.internal.ServerContext
import com.davils.kapi.runtime.server.internal.ServerTelemetry

/**
 * The server of an application that [Kapi] installed: its settings and what serves the operations and channels it
 * mounts.
 *
 * An application gets it with `application.plugin(Kapi)`; generated route classes never need it, since
 * [mount] finds it. A server holds no state per request and is safe to share between threads; its telemetry is bound
 * to the registry and the event bus of its settings when it is installed.
 *
 * @property settings the settings.
 * @since 1.0.0
 */
public class ApiServer internal constructor(public val settings: ServerSettings) {
    internal val context: ServerContext = ServerContext(
        codecs = settings.codecs,
        limits = settings.limits,
        problems = ProblemReplies(settings.codecs, settings.problems),
        clock = settings.clock,
    )

    internal val gate: Gate = Gate(settings.authorizers, settings.realm)

    internal val telemetry: ServerTelemetry = ServerTelemetry(settings.telemetry)

    override fun toString(): String = "ApiServer($settings)"
}
