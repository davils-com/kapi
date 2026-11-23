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

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.ClientSettingsBuilder
import com.davils.kapi.runtime.client.apiClient
import com.davils.kapi.runtime.core.destination.destinationPolicy
import com.davils.kapi.runtime.server.Kapi
import io.ktor.client.HttpClient
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import io.ktor.client.engine.cio.CIO as ClientEngine

private const val HOST: String = "127.0.0.1"

private const val WIRING: String = "com.example.petstore.Wiring"

internal object ServedScenarios {
    suspend fun run(scenario: ServedScenario): Any? = KotlinCompilation.loaded(
        Scenarios.files(scenario.document),
    ) { loader ->
        HttpClient(ClientEngine) { followRedirects = false }.use { http -> served(loader, scenario, http) }
    }

    private suspend fun served(loader: ClassLoader, scenario: ServedScenario, http: HttpClient): Any? {
        val wiring = loader.loadClass(WIRING)
        val instance = wiring.getField("INSTANCE").get(null)
        val mounting = wiring.getMethod("install", Route::class.java)
        val running = embeddedServer(CIO, port = 0, host = HOST) {
            install(Kapi, scenario.server)
            routing { mounting.invoke(instance, this) }
        }
        running.startSuspend(wait = false)
        try {
            val port = running.engine
                .resolvedConnectors()
                .first()
                .port
            val base = "http://$HOST:$port"
            return Scenarios.run(loader, clientOf(http, base, scenario.client), base)
        } finally {
            withContext(NonCancellable) { running.stopSuspend(gracePeriodMillis = 0L, timeoutMillis = 0L) }
        }
    }

    private fun clientOf(http: HttpClient, base: String, configure: ClientSettingsBuilder.() -> Unit): ApiClient =
        apiClient(http) {
            baseUrl = base
            destinations = destinationPolicy {
                schemes = setOf("http")
                hosts = setOf(HOST)
            }
            configure()
        }
}
