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

package com.davils.kapi.spec.openapi.overlay

internal object OverlayKeys {
    const val OVERLAY: String = "overlay"
    const val INFO: String = "info"
    const val TITLE: String = "title"
    const val VERSION: String = "version"
    const val EXTENDS: String = "extends"
    const val ACTIONS: String = "actions"
    const val TARGET: String = "target"
    const val DESCRIPTION: String = "description"
    const val UPDATE: String = "update"
    const val REMOVE: String = "remove"
    const val EXTENSION_PREFIX: String = "x-"

    const val VERSION_PREFIX: String = "1.0."

    val rootFields: Set<String> = setOf(OVERLAY, INFO, EXTENDS, ACTIONS)

    val infoFields: Set<String> = setOf(TITLE, VERSION)

    val actionFields: Set<String> = setOf(TARGET, DESCRIPTION, UPDATE, REMOVE)

    fun isVersion(text: String): Boolean {
        val patch = text.removePrefix(VERSION_PREFIX)
        val isNumber = patch.isNotEmpty() && patch.all { character -> character in '0'..'9' }
        val isCanonical = patch == "0" || !patch.startsWith('0')
        return text.startsWith(VERSION_PREFIX) && isNumber && isCanonical
    }
}
