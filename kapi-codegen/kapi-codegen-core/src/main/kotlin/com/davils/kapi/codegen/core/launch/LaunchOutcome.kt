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
 * How a run of the generator ended.
 *
 * @since 1.0.0
 */
@DavilsInternal
public enum class LaunchOutcome {
    /**
     * The document was read and the code was written.
     *
     * @since 1.0.0
     */
    Generated,

    /**
     * The document or the generator found errors, so nothing was written.
     *
     * @since 1.0.0
     */
    Refused,

    /**
     * The run itself failed, such as on an output that cannot be written.
     *
     * @since 1.0.0
     */
    Failed,
}
