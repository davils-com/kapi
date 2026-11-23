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

package com.davils.kapi.runtime.client.conditional

import com.davils.kapi.runtime.client.call.Exchange

/**
 * How a conditional call of RFC 9110 §13 ended.
 *
 * @since 1.0.0
 */
public sealed interface ConditionalOutcome {
    /**
     * The representation changed, or the call had no condition: the answer carries it with its new validators.
     *
     * @property answered the successful answer.
     * @property validators the validators of the answer.
     * @since 1.0.0
     */
    public class Modified(public val answered: Exchange.Answered, public val validators: Validators) :
        ConditionalOutcome {
        override fun equals(other: Any?): Boolean =
            other is Modified && answered == other.answered && validators == other.validators

        override fun hashCode(): Int = answered.hashCode()

        override fun toString(): String = "Modified($answered, $validators)"
    }

    /**
     * `304 Not Modified`: the representation the client holds is still current.
     *
     * @property validators the validators the server sent with the answer, which may update those the client holds.
     * @since 1.0.0
     */
    public class NotModified(public val validators: Validators) : ConditionalOutcome {
        override fun equals(other: Any?): Boolean = other is NotModified && validators == other.validators

        override fun hashCode(): Int = validators.hashCode()

        override fun toString(): String = "NotModified($validators)"
    }

    /**
     * `412 Precondition Failed`: the representation changed since the client read it, so a change was not made.
     *
     * @property answered the answer.
     * @since 1.0.0
     */
    public class PreconditionFailed(public val answered: Exchange.Answered) : ConditionalOutcome {
        override fun equals(other: Any?): Boolean = other is PreconditionFailed && answered == other.answered

        override fun hashCode(): Int = answered.hashCode()

        override fun toString(): String = "PreconditionFailed($answered)"
    }

    /**
     * Another status or no answer at all.
     *
     * @property exchange how the call ended.
     * @since 1.0.0
     */
    public class Other(public val exchange: Exchange) : ConditionalOutcome {
        override fun equals(other: Any?): Boolean = other is Other && exchange == other.exchange

        override fun hashCode(): Int = exchange.hashCode()

        override fun toString(): String = "Other($exchange)"
    }
}
