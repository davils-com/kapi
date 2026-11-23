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
 * The visibility of the generated declarations.
 *
 * @since 1.0.0
 */
public enum class Visibility {
    /**
     * Public: the generated code is API of its module.
     *
     * @since 1.0.0
     */
    Public,

    /**
     * Internal: the generated code stays inside its module.
     *
     * @since 1.0.0
     */
    Internal,
}
