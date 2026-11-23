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

package com.davils.kapi.runtime.server.webhook

/**
 * Finds the key a sender signs with by the parameter `keyid` of RFC 9421 §2.3; the caller's store of the keys of its
 * senders.
 *
 * The key id comes from the request and may come from an attacker: a resolver only looks it up. It runs contained,
 * and a failure refuses the delivery. It must be safe to call from any coroutine at once.
 *
 * @since 1.0.0
 */
public fun interface SignatureKeyResolver {
    /**
     * Finds the key of [keyId].
     *
     * @param keyId the key id of the signature.
     * @return the key with its algorithm, or `null` if the key is unknown.
     * @since 1.0.0
     */
    public suspend fun resolve(keyId: String): VerificationKey?
}
