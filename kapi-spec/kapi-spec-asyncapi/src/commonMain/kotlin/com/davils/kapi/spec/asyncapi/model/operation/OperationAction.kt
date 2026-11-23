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

package com.davils.kapi.spec.asyncapi.model.operation

/**
 * Whether an operation sends or receives its messages, from the point of view of the application the document
 * describes.
 *
 * @property keyword the value of the field `action`.
 * @since 1.0.0
 */
public enum class OperationAction(public val keyword: String) {
    /**
     * The application sends the messages.
     *
     * @since 1.0.0
     */
    Send("send"),

    /**
     * The application receives the messages.
     *
     * @since 1.0.0
     */
    Receive("receive"),
}
