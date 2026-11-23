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

package com.davils.kapi.spec.schema.suite

import com.davils.kapi.spec.schema.Dialect

enum class SuiteDraft(val folder: String, val dialect: Dialect) {
    Draft4("draft4", Dialect.Draft04),
    Draft6("draft6", Dialect.Draft06),
    Draft7("draft7", Dialect.Draft07),
    Draft201909("draft2019-09", Dialect.Draft201909),
    Draft202012("draft2020-12", Dialect.Draft202012),
}
