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

package com.davils.kapi.spec.core.source

import com.davils.kapi.spec.core.diagnostic.DocumentUri

/**
 * Loads the documents a reading needs, the root document of a [Source.Located] and every document a reference names.
 *
 * kapi itself opens no file and no connection: a resolver of the application decides what it loads. A resolver should
 * refuse what it must not serve, such as a URI outside its root or a remote URI, with [Resolution.Refused]. The reading
 * calls it from its own coroutine, one document at a time, and checks every answer against its limits.
 *
 * @since 1.0.0
 */
public fun interface Resolver {
    /**
     * Loads the document at [uri].
     *
     * @param uri the absolute URI of the document.
     * @return what the resolver found; an exception thrown here is reported as a failed resolution.
     * @since 1.0.0
     */
    public suspend fun resolve(uri: DocumentUri): Resolution

    /**
     * The resolvers kapi brings along.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Refuses every document, so a reading follows only references inside the document it was handed.
         *
         * @since 1.0.0
         */
        public val None: Resolver = Resolver {
            Resolution.Refused(Refusal.Unresolvable)
        }
    }
}
