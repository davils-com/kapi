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

/**
 * For whom a change of an API document must not break: the side a compatibility check judges.
 *
 * @since 1.0.0
 */
public enum class CompatibilitySide {
    /**
     * The clients that call the API, which a removed operation or a tightened request breaks.
     *
     * @since 1.0.0
     */
    Clients,

    /**
     * The providers that serve the API, which a new operation or a widened request breaks.
     *
     * @since 1.0.0
     */
    Providers,

    /**
     * Both sides, for an API whose clients and providers are released apart.
     *
     * @since 1.0.0
     */
    Both,
}
