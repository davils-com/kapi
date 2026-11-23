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

package com.davils.kapi.runtime.core.security

/**
 * A [TokenSource] that can replace a credential the server rejected, such as an OAuth 2 access token that expired.
 *
 * @since 1.0.0
 */
public interface RefreshingTokenSource : TokenSource {
    /**
     * Replaces the credential after the server rejected [rejected].
     *
     * A source should refresh once for many concurrent rejections of the same credential and answer `false` when it
     * cannot get a new one, so a client does not retry forever.
     *
     * @param rejected the credential the server rejected, or `null` if none was sent.
     * @return `true` if [token] now hands out a new credential.
     * @since 1.0.0
     */
    public suspend fun refresh(rejected: Secret?): Boolean
}
