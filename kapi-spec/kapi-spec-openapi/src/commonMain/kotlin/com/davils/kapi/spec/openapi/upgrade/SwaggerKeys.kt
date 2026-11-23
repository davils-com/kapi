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

package com.davils.kapi.spec.openapi.upgrade

internal object SwaggerKeys {
    const val VERSION: String = "2.0"
    const val UPGRADED_VERSION: String = "3.1.1"
    const val HOST: String = "host"
    const val BASE_PATH: String = "basePath"
    const val SCHEMES: String = "schemes"
    const val CONSUMES: String = "consumes"
    const val PRODUCES: String = "produces"
    const val DEFINITIONS: String = "definitions"
    const val SECURITY_DEFINITIONS: String = "securityDefinitions"
    const val COLLECTION_FORMAT: String = "collectionFormat"
    const val NULLABLE: String = "x-nullable"
    const val FLOW: String = "flow"
    const val EXTENSION_PREFIX: String = "x-"
    const val BODY: String = "body"
    const val FORM_DATA: String = "formData"
    const val QUERY: String = "query"
    const val HEADER: String = "header"
    const val FILE: String = "file"
    const val STRING: String = "string"
    const val OBJECT: String = "object"
    const val ARRAY: String = "array"
    const val NULL: String = "null"
    const val BINARY: String = "binary"
    const val BASIC: String = "basic"
    const val OAUTH2: String = "oauth2"
    const val CONTENT_TYPE: String = "content-type"
    const val CSV: String = "csv"
    const val SSV: String = "ssv"
    const val PIPES: String = "pipes"
    const val MULTI: String = "multi"
    const val FORM_STYLE: String = "form"
    const val SIMPLE_STYLE: String = "simple"
    const val SPACE_DELIMITED: String = "spaceDelimited"
    const val PIPE_DELIMITED: String = "pipeDelimited"
    const val JSON: String = "application/json"
    const val MULTIPART: String = "multipart/form-data"
    const val FORM: String = "application/x-www-form-urlencoded"
}
