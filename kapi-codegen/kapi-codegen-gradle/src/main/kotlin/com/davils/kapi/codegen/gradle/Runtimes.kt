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

package com.davils.kapi.codegen.gradle

internal object Runtimes {
    const val GROUP: String = "com.davils"

    const val GENERATOR: String = "kapi-codegen-core"

    private const val CORE: String = "kapi-runtime-core"

    private const val CLIENT: String = "kapi-runtime-client"

    private const val SERVER: String = "kapi-runtime-server"

    private val common: Map<String, String> = mapOf(
        "Client" to CLIENT,
        "Senders" to CLIENT,
    )

    private val jvm: Map<String, String> = mapOf(
        "Server" to SERVER,
        "Mocks" to SERVER,
        "Receivers" to SERVER,
    )

    fun commonOf(artifacts: Set<String>): List<String> = (listOf(CORE) + artifacts.mapNotNull(common::get)).distinct()

    fun jvmOf(artifacts: Set<String>): List<String> = artifacts.mapNotNull(jvm::get).distinct()

    fun notation(module: String, version: String): String = "$GROUP:$module:$version"
}
