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

package com.davils.kapi.codegen.core.fixture

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedFileKind
import com.davils.kapi.runtime.client.ApiClient
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

private const val SCENARIO: String = "com.example.petstore.Scenario"

internal object Scenarios {
    suspend fun files(document: String): List<GeneratedFile> {
        val scenario = GeneratedFile(
            path = SCENARIO.replace(oldChar = '.', newChar = '/') + ".kt",
            kind = GeneratedFileKind.Kotlin,
            content = Golden.text("$document-scenario.txt"),
        )
        return Golden.generated(document).files + scenario
    }

    suspend fun run(loader: ClassLoader, client: ApiClient, base: String): Any? {
        val type = loader.loadClass(SCENARIO)
        val constructor = type.getConstructor(ApiClient::class.java, String::class.java)
        val scenario = constructor.newInstance(client, base)
        val run = type.getMethod("run", Continuation::class.java)
        return suspendCoroutineUninterceptedOrReturn { continuation -> run.invoke(scenario, continuation) }
    }
}
