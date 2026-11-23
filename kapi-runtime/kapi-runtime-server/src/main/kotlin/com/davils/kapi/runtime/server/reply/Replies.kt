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

package com.davils.kapi.runtime.server.reply

/**
 * Builds the reply of a handler.
 *
 * @param status the status code, from 100 to 599.
 * @param configure sets the header fields and the body.
 * @return the reply.
 * @throws IllegalArgumentException if [status] lies outside 100 to 599 or a header field is malformed or reserved.
 * @since 1.0.0
 */
public fun reply(status: Int, configure: ReplyBuilder.() -> Unit = {}): Reply {
    val builder = ReplyBuilder(status)
    builder.configure()
    return builder.build()
}
