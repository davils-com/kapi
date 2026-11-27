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

private const val JSON: String = "application/json"

private const val JSON_SUFFIX: String = "+json"

private val xml: Set<String> = setOf("application/xml", "text/xml")

private const val XML_SUFFIX: String = "+xml"

internal object MediaTypes {
    fun essenceOf(type: String): String {
        val bare = type.substringBefore(';')
        return bare.trim().lowercase()
    }

    fun isJson(type: String): Boolean {
        val essence = essenceOf(type)
        return essence == JSON || essence.endsWith(JSON_SUFFIX)
    }

    fun isXml(type: String): Boolean {
        val essence = essenceOf(type)
        return essence in xml || essence.endsWith(XML_SUFFIX)
    }
}
