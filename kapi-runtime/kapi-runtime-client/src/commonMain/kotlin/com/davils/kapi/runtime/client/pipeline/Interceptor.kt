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

package com.davils.kapi.runtime.client.pipeline

/**
 * Code of the application that sees and changes a call on its way, such as tracing, signing or a cache.
 *
 * An interceptor is foreign code: the client runs it inside `attempted` of Kore, so whatever it throws ends the call
 * with [com.davils.kapi.runtime.client.call.CallFailure.Aborted] and a cancellation of the caller passes on. It is
 * called from the coroutine of the call, concurrently for concurrent calls, and must be safe for that.
 *
 * @since 1.0.0
 */
public fun interface Interceptor {
    /**
     * Returns the round trip to go on with: [trip] itself, one with a changed call, or one with an exchange, which
     * answers the call so the transport does not send it.
     *
     * @param trip the call and how it ended so far.
     * @return the round trip to go on with.
     * @since 1.0.0
     */
    public suspend fun intercept(trip: RoundTrip): RoundTrip
}
