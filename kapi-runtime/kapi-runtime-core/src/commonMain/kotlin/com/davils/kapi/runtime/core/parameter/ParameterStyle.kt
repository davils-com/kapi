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

package com.davils.kapi.runtime.core.parameter

/**
 * The serialization styles of a parameter of OpenAPI 3.2 §4.12.
 *
 * @since 1.0.0
 */
public enum class ParameterStyle {
    /**
     * `simple` of RFC 6570, such as `blue,black`; the default of path parameters and header fields.
     *
     * @since 1.0.0
     */
    Simple,

    /**
     * `label` of RFC 6570, such as `.blue.black`, for path parameters.
     *
     * @since 1.0.0
     */
    Label,

    /**
     * `matrix` of RFC 6570, such as `;color=blue,black`, for path parameters.
     *
     * @since 1.0.0
     */
    Matrix,

    /**
     * `form` of RFC 6570, such as `color=blue,black`; the default of query parameters and cookies.
     *
     * @since 1.0.0
     */
    Form,

    /**
     * `spaceDelimited`, such as `color=blue%20black`, for query parameters.
     *
     * @since 1.0.0
     */
    SpaceDelimited,

    /**
     * `pipeDelimited`, such as `color=blue|black`, for query parameters.
     *
     * @since 1.0.0
     */
    PipeDelimited,

    /**
     * `deepObject`, such as `color[R]=100`, for query parameters.
     *
     * @since 1.0.0
     */
    DeepObject,

    /**
     * `cookie` of OpenAPI 3.2, such as `color=blue; size=2`: like [Form], with the pairs of a `Cookie` field of
     * RFC 6265 and without percent-encoding.
     *
     * @since 1.0.0
     */
    Cookie,
}
