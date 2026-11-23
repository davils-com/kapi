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

public object Keywords {
    public val NAME: ExtensionName = ExtensionName("x-kapi-name")

    public val TYPE: ExtensionName = ExtensionName("x-kapi-type")

    public val IGNORE: ExtensionName = ExtensionName("x-kapi-ignore")

    public val SENSITIVE: ExtensionName = ExtensionName("x-kapi-sensitive")

    public val ENUM_NAMES: ExtensionName = ExtensionName("x-kapi-enum-names")

    public val ENUM_NAMES_ALIAS: ExtensionName = ExtensionName("x-enum-varnames")

    public val GROUP: ExtensionName = ExtensionName("x-kapi-group")

    public val IDEMPOTENT: ExtensionName = ExtensionName("x-kapi-idempotent")

    public val TIMEOUT: ExtensionName = ExtensionName("x-kapi-timeout")

    public val PAGINATION: ExtensionName = ExtensionName("x-kapi-pagination")

    public val TRI_STATE: ExtensionName = ExtensionName("x-kapi-tri-state")

    public val STREAM: ExtensionName = ExtensionName("x-kapi-stream")

    public val UPGRADE: ExtensionName = ExtensionName("x-kapi-upgrade")

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
