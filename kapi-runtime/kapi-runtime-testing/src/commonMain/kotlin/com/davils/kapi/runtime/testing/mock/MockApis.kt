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

package com.davils.kapi.runtime.testing.mock

/**
 * Creates a mock HTTP API on a Ktor `MockEngine`, configured by [configure].
 *
 * ```kotlin
 * val api = mockApi {
 *     basePath = "/v1"
 *     get("/pets/{id}") { respond(200, Pet(pathParameters.getValue("id"))) }
 * }
 * val client = apiClient(api.httpClient()) { baseUrl = "https://api.example.com/v1" }
 * ```
 *
 * @param configure the routes, the base path and the codecs.
 * @return the mock API.
 * @throws com.davils.kore.core.validation.report.ValidationException if the configuration is not valid, such as a
 * path template that does not start with a slash or two routes for the same method and template; its report names
 * every problem.
 * @since 1.0.0
 */
public fun mockApi(configure: MockApiBuilder.() -> Unit): MockApi {
    val builder = MockApiBuilder()
    builder.configure()
    return builder.build()
}
