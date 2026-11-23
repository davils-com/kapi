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

package com.davils.kapi.runtime.client.internal

import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.core.response.Headers
import kotlinx.io.bytestring.ByteString
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

private const val FIRST_SUCCESS: Int = 200

private const val LAST_SUCCESS: Int = 299

private const val SWITCHING: Int = 101

internal class StreamSink(
    val isStreamed: (Int, Headers) -> Boolean = { status, _ -> isOpened(status) },
    val accept: suspend (ByteString) -> CallFailure?,
    val finish: suspend () -> CallFailure? = { null },
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<StreamSink> {
        fun isOpened(status: Int): Boolean = status in FIRST_SUCCESS..LAST_SUCCESS || status == SWITCHING
    }
}
