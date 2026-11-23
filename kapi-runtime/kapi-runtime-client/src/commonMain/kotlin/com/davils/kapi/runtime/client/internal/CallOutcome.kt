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

import com.davils.kapi.runtime.client.call.Exchange

private const val CLIENT_ERRORS: Int = 400

private const val SERVER_ERRORS: Int = 500

internal enum class CallOutcome(val tag: String) {
    Success("success"),
    ClientError("client_error"),
    ServerError("server_error"),
    Failed("failed"),
    ;

    companion object {
        fun of(exchange: Exchange): CallOutcome = when (exchange) {
            is Exchange.Failed -> Failed
            is Exchange.Answered -> of(exchange.status)
        }

        private fun of(status: Int): CallOutcome {
            if (status >= SERVER_ERRORS) return ServerError
            return ClientError.takeIf { status >= CLIENT_ERRORS } ?: Success
        }
    }
}
