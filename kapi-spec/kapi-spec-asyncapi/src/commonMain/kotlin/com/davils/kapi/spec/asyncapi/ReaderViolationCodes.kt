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

package com.davils.kapi.spec.asyncapi

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations an AsyncAPI reader configuration reports when it is built, next to the codes of
 * [com.davils.kapi.spec.core.policy.PolicyCodes].
 *
 * @since 1.0.0
 */
public object ReaderViolationCodes {
    /**
     * A downgrade names a code that keeps its severity because it guards the reader itself.
     *
     * @since 1.0.0
     */
    public val SEVERITY_FIXED: ViolationCode = ViolationCode("asyncapi.reader.severity-fixed")
}
