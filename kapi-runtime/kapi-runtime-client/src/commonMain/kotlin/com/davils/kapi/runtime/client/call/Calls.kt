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

package com.davils.kapi.runtime.client.call

/**
 * Builds the call of the operation [operation] with the method [method].
 *
 * @param operation the `operationId`, which names the call in telemetry.
 * @param method the method, such as `GET`; it is written in upper case.
 * @param configure adds the path, the parameters, the content and the security requirement.
 * @return the call; one that cannot be sent safely carries its [Call.refusal].
 * @since 1.0.0
 */
public fun apiCall(operation: String, method: String, configure: CallBuilder.() -> Unit = {}): Call {
    val builder = CallBuilder(operation = operation, method = method.uppercase())
    builder.configure()
    return builder.build()
}
