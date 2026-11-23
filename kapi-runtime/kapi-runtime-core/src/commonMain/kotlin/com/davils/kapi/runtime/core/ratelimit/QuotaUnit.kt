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

package com.davils.kapi.runtime.core.ratelimit

/**
 * What a quota of draft-ietf-httpapi-ratelimit-headers counts, the parameter `qu` of a policy.
 *
 * @property value the name the field writes.
 * @since 1.0.0
 */
public enum class QuotaUnit(public val value: String) {
    /**
     * Requests, the default.
     *
     * @since 1.0.0
     */
    Requests("requests"),

    /**
     * Bytes of content.
     *
     * @since 1.0.0
     */
    ContentBytes("content-bytes"),

    /**
     * Requests served at the same time.
     *
     * @since 1.0.0
     */
    ConcurrentRequests("concurrent-requests"),
}
