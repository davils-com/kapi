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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.spec.asyncapi.load

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.core.load.LoadCodes
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode

internal object LoadDiagnostics : LoadCodes {
    override val documentEmpty: ViolationCode get() = DiagnosticCodes.DOCUMENT_NOT_MAPPING

    override val tooManyDocuments: ViolationCode get() = DiagnosticCodes.LOAD_DOCUMENTS

    override val failed: ViolationCode get() = DiagnosticCodes.LOAD_FAILED

    override val missing: ViolationCode get() = DiagnosticCodes.LOAD_MISSING

    override val remote: ViolationCode get() = DiagnosticCodes.LOAD_REMOTE

    override val scheme: ViolationCode get() = DiagnosticCodes.LOAD_SCHEME

    override val size: ViolationCode get() = DiagnosticCodes.LOAD_SIZE

    override val outside: ViolationCode get() = DiagnosticCodes.REFERENCE_OUTSIDE

    override val encoding: ViolationCode get() = DiagnosticCodes.SOURCE_ENCODING

    override val multipleDocuments: ViolationCode get() = DiagnosticCodes.SOURCE_MULTIPLE_DOCUMENTS
}
