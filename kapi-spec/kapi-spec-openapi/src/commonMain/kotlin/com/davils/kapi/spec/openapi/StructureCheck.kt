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
     * The schemas are the ones the OpenAPI Initiative publishes at `https://spec.openapis.org`, embedded unchanged:
     * `oas/3.0/schema/2024-10-18`, `oas/3.1/schema/2026-11-05` and `oas/3.2/schema/2026-11-12`; the schemas of 3.1
     * and 3.2 leave out the Schema Objects, which the reader checks itself. A break is reported as
     * [DiagnosticCodes.STRUCTURE_SCHEMA] at the deepest place the schema names, and only where the reader has not
     * reported a finding at that place, below it, above it or beside it already. The validation runs within the
     * limits of the reading; a document beyond them is reported as one finding of the same code instead of being
     * checked in part. Only the root document of a reading is validated, after its overlays are applied.
     *
     * @since 1.0.0
     */
    ReaderAndOfficialSchema,
}
