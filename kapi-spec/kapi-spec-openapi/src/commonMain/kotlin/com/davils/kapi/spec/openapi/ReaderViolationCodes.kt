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

package com.davils.kapi.spec.openapi

import com.davils.kore.core.validation.violation.ViolationCode

public object ReaderViolationCodes {
    public val NOT_POSITIVE: ViolationCode = ViolationCode("openapi.reader.not-positive")

    public val ABOVE_HARD_LIMIT: ViolationCode = ViolationCode("openapi.reader.above-hard-limit")

    public val SEVERITY_FIXED: ViolationCode = ViolationCode("openapi.reader.severity-fixed")

    public val REASON_MISSING: ViolationCode = ViolationCode("openapi.reader.reason-missing")

    public val ROOT_NOT_DIRECTORY: ViolationCode = ViolationCode("openapi.reader.root-not-directory")
}
