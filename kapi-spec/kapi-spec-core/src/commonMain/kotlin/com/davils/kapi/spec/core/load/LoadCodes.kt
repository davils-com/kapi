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

package com.davils.kapi.spec.core.load

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes a [DocumentLoader] reports, which every reader names in its own namespace, such as
 * `openapi.load.missing`.
 *
 * @since 1.0.0
 */
@DavilsInternal
public interface LoadCodes {
    /**
     * A source holds no document.
     *
     * @since 1.0.0
     */
    public val documentEmpty: ViolationCode

    /**
     * A reading would load more documents than its limit.
     *
     * @since 1.0.0
     */
    public val tooManyDocuments: ViolationCode

    /**
     * The resolver cannot load a document.
     *
     * @since 1.0.0
     */
    public val failed: ViolationCode

    /**
     * A document does not exist.
     *
     * @since 1.0.0
     */
    public val missing: ViolationCode

    /**
     * A document is remote.
     *
     * @since 1.0.0
     */
    public val remote: ViolationCode

    /**
     * A document has another scheme than the root document.
     *
     * @since 1.0.0
     */
    public val scheme: ViolationCode

    /**
     * A document is larger than its limit.
     *
     * @since 1.0.0
     */
    public val size: ViolationCode

    /**
     * A document lies outside the folder references may reach.
     *
     * @since 1.0.0
     */
    public val outside: ViolationCode

    /**
     * A document is no valid UTF-8.
     *
     * @since 1.0.0
     */
    public val encoding: ViolationCode

    /**
     * A source holds several documents.
     *
     * @since 1.0.0
     */
    public val multipleDocuments: ViolationCode
}
