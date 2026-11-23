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

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures a [MockApi] in the block of [mockApi].
 *
 * A builder is not thread-safe; it is meant for one block of the DSL.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class MockApiBuilder internal constructor() : ValidatingBuilder<MockApi>() {
    private val added: MutableList<Route> = mutableListOf()

    override val subject: String get() = "mock api"

    internal val routes: List<Route> get() = added.toList()

    /**
     * The path in front of every route, such as `/v1` for a base URL of `https://api.example.com/v1`; empty by
     * default. A request whose path does not start with it matches no route.
     *
     * @since 1.0.0
     */
    public var basePath: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The codecs that write and read bodies; [Codecs.Default] by default, the codecs of a client runtime that is
     * not configured otherwise.
     *
     * @since 1.0.0
     */
    public var codecs: Codecs = Codecs.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Answers requests of [method] to [path] with [answer].
     *
     * Of the routes whose template matches, the one with the most literal characters answers, so `/pets/mine` wins
     * over `/pets/{id}`. A route runs contained: what it throws is recorded in [MockApi.failures] and answered with
     * `500`.
     *
     * @param method the method, such as `GET`, compared exactly, because methods are case-sensitive.
     * @param path the path template, such as `/pets/{id}`, below [basePath].
     * @param answer the answer, which runs once per request.
     * @throws IllegalStateException if the mock API was built already.
     * @since 1.0.0
     */
    public fun on(method: String, path: String, answer: suspend Responder.() -> Unit) {
        checkOpen()
        added += Route(method = method, path = path, answer = answer)
    }

    /**
     * Answers `GET` requests to [path] with [answer], as [on] does.
     *
     * @param path the path template, such as `/pets/{id}`, below [basePath].
     * @param answer the answer, which runs once per request.
     * @throws IllegalStateException if the mock API was built already.
     * @since 1.0.0
     */
    public fun get(path: String, answer: suspend Responder.() -> Unit) {
        checkOpen()
        on(method = "GET", path = path, answer = answer)
    }

    /**
     * Answers `POST` requests to [path] with [answer], as [on] does.
     *
     * @param path the path template, such as `/pets/{id}`, below [basePath].
     * @param answer the answer, which runs once per request.
     * @throws IllegalStateException if the mock API was built already.
     * @since 1.0.0
     */
    public fun post(path: String, answer: suspend Responder.() -> Unit) {
        checkOpen()
        on(method = "POST", path = path, answer = answer)
    }

    /**
     * Answers `PUT` requests to [path] with [answer], as [on] does.
     *
     * @param path the path template, such as `/pets/{id}`, below [basePath].
     * @param answer the answer, which runs once per request.
     * @throws IllegalStateException if the mock API was built already.
     * @since 1.0.0
     */
    public fun put(path: String, answer: suspend Responder.() -> Unit) {
        checkOpen()
        on(method = "PUT", path = path, answer = answer)
    }

    /**
     * Answers `PATCH` requests to [path] with [answer], as [on] does.
     *
     * @param path the path template, such as `/pets/{id}`, below [basePath].
     * @param answer the answer, which runs once per request.
     * @throws IllegalStateException if the mock API was built already.
     * @since 1.0.0
     */
    public fun patch(path: String, answer: suspend Responder.() -> Unit) {
        checkOpen()
        on(method = "PATCH", path = path, answer = answer)
    }

    /**
     * Answers `DELETE` requests to [path] with [answer], as [on] does.
     *
     * @param path the path template, such as `/pets/{id}`, below [basePath].
     * @param answer the answer, which runs once per request.
     * @throws IllegalStateException if the mock API was built already.
     * @since 1.0.0
     */
    public fun delete(path: String, answer: suspend Responder.() -> Unit) {
        checkOpen()
        on(method = "DELETE", path = path, answer = answer)
    }

    override fun assemble(): MockApi = MockApi(this)
}
