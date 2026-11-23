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
 * The phases a call passes, in their order; the interceptors of one phase run in the order they were added.
 *
 * @since 1.0.0
 */
public enum class CallPhase {
    /**
     * Before the credentials are placed: change the call, or answer it from a cache so it is not sent.
     *
     * @since 1.0.0
     */
    Prepare,

    /**
     * After the client placed the credentials, which a call without a credential it needs never passes: sign the
     * call or see the secret of each scheme.
     *
     * @since 1.0.0
     */
    Authorize,

    /**
     * Right before the transport sends the call, which it skips when an interceptor answered it.
     *
     * @since 1.0.0
     */
    Send,

    /**
     * After the transport sent the call: see or change how it ended.
     *
     * @since 1.0.0
     */
    Receive,
}
