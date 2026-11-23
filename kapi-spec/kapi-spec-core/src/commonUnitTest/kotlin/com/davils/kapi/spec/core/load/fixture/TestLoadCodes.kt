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

package com.davils.kapi.spec.core.load.fixture

import com.davils.kapi.spec.core.load.LoadCodes
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode

@OptIn(DavilsInternal::class)
internal object TestLoadCodes : LoadCodes {
    override val documentEmpty: ViolationCode = ViolationCode("test.load.document-empty")
    override val tooManyDocuments: ViolationCode = ViolationCode("test.load.too-many-documents")
    override val failed: ViolationCode = ViolationCode("test.load.failed")
    override val missing: ViolationCode = ViolationCode("test.load.missing")
    override val remote: ViolationCode = ViolationCode("test.load.remote")
    override val scheme: ViolationCode = ViolationCode("test.load.scheme")
    override val size: ViolationCode = ViolationCode("test.load.size")
    override val outside: ViolationCode = ViolationCode("test.load.outside")
    override val encoding: ViolationCode = ViolationCode("test.load.encoding")
    override val multipleDocuments: ViolationCode = ViolationCode("test.load.multiple-documents")
}
