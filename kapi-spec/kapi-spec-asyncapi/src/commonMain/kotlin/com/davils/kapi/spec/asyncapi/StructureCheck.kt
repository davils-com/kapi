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
     * The reader also validates the document against the official JSON Schema of AsyncAPI 3.0, which catches what the
     * reader does not model, such as the fields of the bindings of every protocol.
     *
     * The schema is the one the AsyncAPI Initiative publishes in `asyncapi/spec-json-schemas`, release 6.11.1,
     * `schemas/3.0.0-without-$id.json`, embedded unchanged: it bundles every definition, the schemas of the bindings
     * and the JSON Schema draft-07 of the Schema Object. A break is reported as [DiagnosticCodes.STRUCTURE_SCHEMA] at
     * the deepest place the schema names, and only where the reader has not reported a finding at that place, below
     * it, above it or beside it already. A Reference Object and a boolean Schema Object are never reported: the reader
     * resolves a reference and checks its target, while the official schema lets a reference match both alternatives
     * of a `oneOf` of a schema and a reference, and refuses the boolean schemas AsyncAPI allows. The validation runs
     * within the limits of the reading; a document beyond them is reported as one finding of the same code instead of
     * being checked in part. Only the root document of a reading is validated.
     *
     * @since 1.0.0
     */
    ReaderAndOfficialSchema,
}
