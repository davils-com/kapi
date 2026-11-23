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

/**
 * How thoroughly a reader checks the structure of a document.
 *
 * @since 1.0.0
 */
public enum class StructureCheck {
    /**
     * The reader checks every field it reads and every rule of the specification.
     *
     * @since 1.0.0
     */
    Reader,

    /**
     * The reader also validates the document against the official JSON Schema of its version of OpenAPI, which
     * catches what the reader does not model.
     *
     * @since 1.0.0
     */
    ReaderAndOfficialSchema,
}
