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

package com.davils.kapi.runtime.server

import io.ktor.server.application.Application
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.util.AttributeKey

/**
 * The Ktor application plugin of kapi, which generated servers run on: `install(Kapi) { … }` configures the server of
 * an application, and `routing { mount(PetsRoutes(service)) }` serves the operations and channels of a generated
 * route class on it.
 *
 * Every mounted operation is a route handler of Ktor on its method and path template, so the plugins and the route
 * scoped plugins of the application run around it as they run around any other route: interceptors are written with
 * the plugin API of Ktor, such as `createRouteScopedPlugin` with its hooks, and installed on a route above the mounted
 * ones. Compression of replies is left to the `Compression` plugin of Ktor; compressed request bodies are inflated by
 * the server within its limits. WebSocket channels need the `WebSockets` plugin of Ktor. The configuration is checked
 * when the plugin is installed and the server is immutable afterwards.
 *
 * @since 1.0.0
 */
public object Kapi : BaseApplicationPlugin<Application, ServerSettingsBuilder, ApiServer> {
    /**
     * The key of the server in the attributes of the application.
     *
     * @since 1.0.0
     */
    override val key: AttributeKey<ApiServer> = AttributeKey("kapi")

    /**
     * Builds the server of [pipeline].
     *
     * @param pipeline the application.
     * @param configure configures the server.
     * @return the server.
     * @throws com.davils.kore.core.validation.report.ValidationException if a setting is invalid.
     * @since 1.0.0
     */
    override fun install(pipeline: Application, configure: ServerSettingsBuilder.() -> Unit): ApiServer {
        val builder = ServerSettingsBuilder()
        builder.configure()
        return ApiServer(builder.build())
    }
}
