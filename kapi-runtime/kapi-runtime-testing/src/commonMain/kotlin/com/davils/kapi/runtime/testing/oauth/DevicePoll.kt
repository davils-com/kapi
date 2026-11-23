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

package com.davils.kapi.runtime.testing.oauth

/**
 * The answer of a [FakeAuthorizationServer] to one poll of the device authorization grant of RFC 8628, before it grants
 * the token.
 *
 * @property error the `error` of the answer.
 * @since 1.0.0
 */
public enum class DevicePoll(public val error: String) {
    /**
     * The user has not finished yet; the client keeps polling.
     *
     * @since 1.0.0
     */
    Pending("authorization_pending"),

    /**
     * The client polls too often; it keeps polling five seconds slower.
     *
     * @since 1.0.0
     */
    SlowDown("slow_down"),

    /**
     * The user denied the authorization; the client stops.
     *
     * @since 1.0.0
     */
    Denied("access_denied"),

    /**
     * The device code expired; the client stops.
     *
     * @since 1.0.0
     */
    Expired("expired_token"),
}
