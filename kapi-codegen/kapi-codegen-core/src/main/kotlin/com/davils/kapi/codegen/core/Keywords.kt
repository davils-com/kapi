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

package com.davils.kapi.codegen.core

import com.davils.kapi.spec.core.value.ExtensionName

/**
 * The extension keywords of documents that the generator reads.
 *
 * @since 1.0.0
 */
public object Keywords {
    /**
     * `x-kapi-name`: the Kotlin name of a schema, property, parameter, field or operation.
     *
     * @since 1.0.0
     */
    public val NAME: ExtensionName = ExtensionName("x-kapi-name")

    /**
     * `x-kapi-type`: a Kotlin type of the application that a schema stands for, with its serializer.
     *
     * @since 1.0.0
     */
    public val TYPE: ExtensionName = ExtensionName("x-kapi-type")

    /**
     * `x-kapi-ignore`: leaves a property, an operation or a schema out of the generated code.
     *
     * @since 1.0.0
     */
    public val IGNORE: ExtensionName = ExtensionName("x-kapi-ignore")

    /**
     * `x-kapi-sensitive`: masks a property in the `toString` of its model.
     *
     * @since 1.0.0
     */
    public val SENSITIVE: ExtensionName = ExtensionName("x-kapi-sensitive")

    /**
     * `x-kapi-enum-names`: the names of the constants of an enumeration.
     *
     * @since 1.0.0
     */
    public val ENUM_NAMES: ExtensionName = ExtensionName("x-kapi-enum-names")

    /**
     * `x-enum-varnames`: the names of the constants of an enumeration, as other generators write them.
     *
     * @since 1.0.0
     */
    public val ENUM_NAMES_ALIAS: ExtensionName = ExtensionName("x-enum-varnames")

    /**
     * `x-kapi-group`: the client and the service an operation belongs to, instead of its first tag.
     *
     * @since 1.0.0
     */
    public val GROUP: ExtensionName = ExtensionName("x-kapi-group")

    /**
     * `x-kapi-idempotent`: marks an operation that may be sent again.
     *
     * @since 1.0.0
     */
    public val IDEMPOTENT: ExtensionName = ExtensionName("x-kapi-idempotent")

    /**
     * `x-kapi-timeout`: the timeouts of the calls of an operation.
     *
     * @since 1.0.0
     */
    public val TIMEOUT: ExtensionName = ExtensionName("x-kapi-timeout")

    /**
     * `x-kapi-pagination`: how the pages of an operation follow each other.
     *
     * @since 1.0.0
     */
    public val PAGINATION: ExtensionName = ExtensionName("x-kapi-pagination")

    /**
     * `x-kapi-tri-state`: a body that tells an absent property from a null one, for merge patches.
     *
     * @since 1.0.0
     */
    public val TRI_STATE: ExtensionName = ExtensionName("x-kapi-tri-state")

    /**
     * `x-kapi-stream`: reads a response as records, events or bytes.
     *
     * @since 1.0.0
     */
    public val STREAM: ExtensionName = ExtensionName("x-kapi-stream")

    /**
     * `x-kapi-upgrade`: the protocol an operation switches the connection to.
     *
     * @since 1.0.0
     */
    public val UPGRADE: ExtensionName = ExtensionName("x-kapi-upgrade")

    /**
     * Every extension keyword the generator reads.
     *
     * @since 1.0.0
     */
    public val all: List<ExtensionName> = listOf(
        NAME,
        TYPE,
        IGNORE,
        SENSITIVE,
        ENUM_NAMES,
        ENUM_NAMES_ALIAS,
        GROUP,
        IDEMPOTENT,
        TIMEOUT,
        PAGINATION,
        TRI_STATE,
        STREAM,
        UPGRADE,
    )
}
