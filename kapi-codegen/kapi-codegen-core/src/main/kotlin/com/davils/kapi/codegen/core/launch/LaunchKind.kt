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

package com.davils.kapi.codegen.core.launch

import com.davils.kore.core.annotation.DavilsInternal

/**
 * The specification a document of a run follows, which decides the reader and the generator.
 *
 * @since 1.0.0
 */
@DavilsInternal
public enum class LaunchKind {
    /**
     * An OpenAPI document of version 3.0, 3.1 or 3.2, or Swagger 2.0, read with its overlays.
     *
     * @since 1.0.0
     */
    OpenApi,

    /**
     * An AsyncAPI 3 document, whose WebSocket channels are generated; it takes no overlays.
     *
     * @since 1.0.0
     */
    AsyncApi,
}
