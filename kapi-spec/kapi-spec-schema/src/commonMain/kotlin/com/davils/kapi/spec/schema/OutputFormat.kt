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

package com.davils.kapi.spec.schema

/**
 * How much a [Validation] reports, after the output formats of JSON Schema 2020-12.
 *
 * @since 1.0.0
 */
public enum class OutputFormat {
    /**
     * Only whether the value is valid: [Validation.Invalid.errors] is empty, and evaluation stops at the first
     * failing keyword of each subschema, which makes it the fastest format.
     *
     * @since 1.0.0
     */
    Flag,

    /**
     * Every failing keyword as one flat list of [OutputUnit]s, the "basic" structure of JSON Schema 2020-12.
     *
     * @since 1.0.0
     */
    Basic,
}
