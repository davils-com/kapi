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

package com.davils.kapi.codegen.core.options

/**
 * What a generation emits.
 *
 * @since 1.0.0
 */
public enum class Artifact {
    /**
     * The models of the schemas, common Kotlin with kotlinx.serialization.
     *
     * @since 1.0.0
     */
    Models,

    /**
     * A client per group of operations on the client runtime, common Kotlin.
     *
     * @since 1.0.0
     */
    Client,

    /**
     * Routes and a service per group of operations on the server runtime, for the JVM.
     *
     * @since 1.0.0
     */
    Server,

    /**
     * Services that answer every operation with samples of its documented success, for the JVM.
     *
     * @since 1.0.0
     */
    Mocks,

    /**
     * Clients that deliver the webhooks and callbacks of the document, common Kotlin.
     *
     * @since 1.0.0
     */
    Senders,

    /**
     * Routes that receive the webhooks and callbacks of the document, for the JVM.
     *
     * @since 1.0.0
     */
    Receivers,
}
