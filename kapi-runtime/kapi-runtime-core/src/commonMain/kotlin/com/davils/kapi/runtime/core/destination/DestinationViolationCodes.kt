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

package com.davils.kapi.runtime.core.destination

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations a [DestinationPolicy] reports when it is configured.
 *
 * @since 1.0.0
 */
public object DestinationViolationCodes {
    /**
     * The policy names no scheme.
     *
     * @since 1.0.0
     */
    public val SCHEMES_EMPTY: ViolationCode = ViolationCode("kapi.runtime.destination.schemes-empty")

    /**
     * A scheme is not written in lower case as RFC 3986 §3.1 spells one.
     *
     * @since 1.0.0
     */
    public val SCHEME_INVALID: ViolationCode = ViolationCode("kapi.runtime.destination.scheme-invalid")

    /**
     * A host is neither a host in lower case nor one that starts with `*.`.
     *
     * @since 1.0.0
     */
    public val HOST_INVALID: ViolationCode = ViolationCode("kapi.runtime.destination.host-invalid")
}
