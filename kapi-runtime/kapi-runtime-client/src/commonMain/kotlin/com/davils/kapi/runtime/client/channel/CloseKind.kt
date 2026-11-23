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

package com.davils.kapi.runtime.client.channel

private const val NORMAL: Int = 1_000

private const val GOING_AWAY: Int = 1_001

private const val PROTOCOL_ERROR: Int = 1_002

private const val UNSUPPORTED_DATA: Int = 1_003

private const val NO_STATUS: Int = 1_005

private const val ABNORMAL: Int = 1_006

private const val INVALID_PAYLOAD: Int = 1_007

private const val POLICY_VIOLATION: Int = 1_008

private const val TOO_BIG: Int = 1_009

private const val MANDATORY_EXTENSION: Int = 1_010

private const val INTERNAL_ERROR: Int = 1_011

private const val SERVICE_RESTART: Int = 1_012

private const val TRY_AGAIN_LATER: Int = 1_013

/**
 * The kinds of the close codes of RFC 6455 §7.4.1 and of the IANA registry.
 *
 * @property code the code, or 0 for [Other].
 * @since 1.0.0
 */
public enum class CloseKind(public val code: Int) {
    /**
     * 1000: the purpose of the channel is fulfilled.
     *
     * @since 1.0.0
     */
    Normal(NORMAL),

    /**
     * 1001: the endpoint is going away, such as a server going down.
     *
     * @since 1.0.0
     */
    GoingAway(GOING_AWAY),

    /**
     * 1002: the endpoint broke the protocol.
     *
     * @since 1.0.0
     */
    ProtocolError(PROTOCOL_ERROR),

    /**
     * 1003: the endpoint received a kind of data it cannot accept.
     *
     * @since 1.0.0
     */
    UnsupportedData(UNSUPPORTED_DATA),

    /**
     * 1005: the close frame carried no code.
     *
     * @since 1.0.0
     */
    NoStatus(NO_STATUS),

    /**
     * 1006: the connection ended without a close frame.
     *
     * @since 1.0.0
     */
    Abnormal(ABNORMAL),

    /**
     * 1007: a message was not what its type declares, such as text that is no UTF-8 or a message the codec cannot read.
     *
     * @since 1.0.0
     */
    InvalidPayload(INVALID_PAYLOAD),

    /**
     * 1008: a message broke a policy of the endpoint.
     *
     * @since 1.0.0
     */
    PolicyViolation(POLICY_VIOLATION),

    /**
     * 1009: a frame or a message was larger than the endpoint reads.
     *
     * @since 1.0.0
     */
    TooBig(TOO_BIG),

    /**
     * 1010: the server did not agree to an extension the client needs.
     *
     * @since 1.0.0
     */
    MandatoryExtension(MANDATORY_EXTENSION),

    /**
     * 1011: the server met an unexpected condition.
     *
     * @since 1.0.0
     */
    InternalError(INTERNAL_ERROR),

    /**
     * 1012: the service restarts.
     *
     * @since 1.0.0
     */
    ServiceRestart(SERVICE_RESTART),

    /**
     * 1013: the server is overloaded; try again later.
     *
     * @since 1.0.0
     */
    TryAgainLater(TRY_AGAIN_LATER),

    /**
     * Any other code, such as one of the range 4000 to 4999 an application defines.
     *
     * @since 1.0.0
     */
    Other(0),
    ;

    /**
     * Finds kinds.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the kind of [code].
         *
         * @param code the code.
         * @return its kind, or [Other].
         * @since 1.0.0
         */
        public fun of(code: Int): CloseKind {
            val known = entries.firstOrNull { kind -> kind.code == code && kind != Other }
            return known ?: Other
        }
    }
}
