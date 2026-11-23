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

package com.davils.kapi.runtime.core.framing

/**
 * Why a reader of a framed stream failed; a reader that failed stays failed.
 *
 * @since 1.0.0
 */
public enum class FramingFailure {
    /**
     * A record or a line is longer than [FramingLimits.maxRecordBytes].
     *
     * @since 1.0.0
     */
    RecordTooLong,

    /**
     * The data of an event grew beyond [FramingLimits.maxEventBytes].
     *
     * @since 1.0.0
     */
    EventTooLong,

    /**
     * A record is no UTF-8.
     *
     * @since 1.0.0
     */
    NotUtf8,
}
