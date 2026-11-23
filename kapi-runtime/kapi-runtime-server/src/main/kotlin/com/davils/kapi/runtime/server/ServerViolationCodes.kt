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

package com.davils.kapi.runtime.server

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations the settings of a server report when they are configured.
 *
 * The codes are stable API: they never change their meaning once released.
 *
 * @since 1.0.0
 */
public object ServerViolationCodes {
    /**
     * The realm of the challenges is empty or holds a character a quoted string cannot carry.
     *
     * @since 1.0.0
     */
    public val REALM_INVALID: ViolationCode = ViolationCode("kapi.server.realm-invalid")

    /**
     * The base of the types of problems is no URI of visible ASCII characters.
     *
     * @since 1.0.0
     */
    public val TYPE_BASE_INVALID: ViolationCode = ViolationCode("kapi.server.type-base-invalid")

    /**
     * A security scheme is given an authorizer twice, or its name is no token.
     *
     * @since 1.0.0
     */
    public val SCHEME_INVALID: ViolationCode = ViolationCode("kapi.server.scheme-invalid")

    /**
     * A duration of the settings is not positive.
     *
     * @since 1.0.0
     */
    public val DURATION_INVALID: ViolationCode = ViolationCode("kapi.server.duration-invalid")
}
