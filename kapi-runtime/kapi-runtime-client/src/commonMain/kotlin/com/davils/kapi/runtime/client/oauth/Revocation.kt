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

package com.davils.kapi.runtime.client.oauth

/**
 * How a revocation of RFC 7009 ended.
 *
 * @since 1.0.0
 */
public enum class Revocation {
    /**
     * The server revoked the token, or did not know it, and the source forgot its tokens.
     *
     * @since 1.0.0
     */
    Revoked,

    /**
     * The source held no token to revoke.
     *
     * @since 1.0.0
     */
    NothingHeld,

    /**
     * Neither the settings nor the metadata of the issuer name a revocation endpoint.
     *
     * @since 1.0.0
     */
    Unsupported,

    /**
     * The server refused, could not be reached or did not answer in time; the source keeps its tokens.
     *
     * @since 1.0.0
     */
    Failed,
}
