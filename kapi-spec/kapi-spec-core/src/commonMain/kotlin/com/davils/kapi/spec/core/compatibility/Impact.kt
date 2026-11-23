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

package com.davils.kapi.spec.core.compatibility

/**
 * What a change between two versions of a document means for one side of the API.
 *
 * A client is code written against the baseline that now talks to a provider of the candidate; a provider is an
 * implementation of the baseline that now serves clients of the candidate.
 *
 * @since 1.0.0
 */
public enum class Impact {
    /**
     * The side may fail: a client may send what the candidate refuses or receive what it cannot read, a provider may
     * receive what it cannot handle or send what the candidate no longer allows.
     *
     * @since 1.0.0
     */
    Breaking,

    /**
     * The side keeps working, although the document changed in a way it can notice.
     *
     * @since 1.0.0
     */
    NonBreaking,

    /**
     * The change carries no risk for the side but is worth knowing, such as a deprecation.
     *
     * @since 1.0.0
     */
    Info,
}
