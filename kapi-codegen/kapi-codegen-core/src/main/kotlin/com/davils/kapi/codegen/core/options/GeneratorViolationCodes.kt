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


package com.davils.kapi.codegen.core.options

import com.davils.kore.core.validation.violation.ViolationCode

public object GeneratorViolationCodes {
    public val PACKAGE_INVALID: ViolationCode = code("package-invalid")

    public val API_NAME_INVALID: ViolationCode = code("api-name-invalid")

    public val TYPE_PREFIX_INVALID: ViolationCode = code("type-prefix-invalid")

    public val ARTIFACTS_EMPTY: ViolationCode = code("artifacts-empty")

    public val ARTIFACT_MISSING: ViolationCode = code("artifact-missing")

    private fun code(name: String): ViolationCode = ViolationCode("codegen.options.$name")
}
