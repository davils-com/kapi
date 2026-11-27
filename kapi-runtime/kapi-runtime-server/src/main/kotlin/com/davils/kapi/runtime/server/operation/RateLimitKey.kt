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

package com.davils.kapi.runtime.server.operation

/**
 * Whose requests one budget of an [OperationRateLimit] counts.
 *
 * @since 1.2.0
 */
public enum class RateLimitKey {
    /**
     * The caller: its principal once authorized, or else the address of its peer, as the rate limit of the server
     * counts.
     *
     * @since 1.2.0
     */
    Caller,

    /**
     * The principal of the request; every request without a principal shares one budget.
     *
     * @since 1.2.0
     */
    Principal,

    /**
     * The address of the peer, whether the request has a principal or not; every request without an address shares
     * one budget.
     *
     * @since 1.2.0
     */
    Address,
}
