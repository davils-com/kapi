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

package com.davils.kapi.spec.asyncapi.model.schema

/**
 * The format a schema is written in, recognised by its `schemaFormat`, such as
 * `application/vnd.aai.asyncapi+json;version=3.0.0`.
 *
 * The media type is compared without its case and without blanks; the `version` parameter has to name a version of
 * the format kapi knows where the format has versions that differ, such as `draft-07` of JSON Schema.
 *
 * @since 1.0.0
 */
public enum class SchemaFormat {
    /**
     * The schema of AsyncAPI, a superset of JSON Schema draft 07, which a schema without a `schemaFormat` is written
     * in: `application/vnd.aai.asyncapi`, also with `+json` or `+yaml`.
     *
     * @since 1.0.0
     */
    AsyncApi,

    /**
     * JSON Schema draft 07: `application/schema+json;version=draft-07` or `application/schema+yaml;version=draft-07`.
     *
     * @since 1.0.0
     */
    JsonSchema,

    /**
     * The schema of OpenAPI 3.0: `application/vnd.oai.openapi;version=3.0.0`, also with `+json` or `+yaml`.
     *
     * @since 1.0.0
     */
    OpenApi,

    /**
     * Apache Avro 1: `application/vnd.apache.avro;version=1.9.0`, also with `+json` or `+yaml`.
     *
     * @since 1.0.0
     */
    Avro,

    /**
     * The data types of RAML 1.0: `application/raml+yaml;version=1.0`.
     *
     * @since 1.0.0
     */
    Raml,

    /**
     * Protocol Buffers, written inline as a string: `application/vnd.google.protobuf;version=3`.
     *
     * @since 1.0.0
     */
    Protobuf,

    /**
     * A format kapi does not know; the reading reports it as a warning and keeps the schema as it was written.
     *
     * @since 1.0.0
     */
    Unknown,
}
