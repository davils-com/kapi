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

package com.davils.kapi.spec.asyncapi

/**
 * A line of AsyncAPI versions that share one set of fields.
 *
 * The entries are ordered from the oldest to the newest line. kapi reads AsyncAPI 3.0 and is prepared for the later
 * lines of AsyncAPI 3.
 *
 * @property prefix the major and minor version of the line, such as `3.0`.
 * @since 1.0.0
 */
public enum class VersionFamily(public val prefix: String) {
    /**
     * AsyncAPI 3.0, which separates channels from operations, references the messages of a channel from its
     * operations and describes replies.
     *
     * @since 1.0.0
     */
    V30("3.0"),
}
