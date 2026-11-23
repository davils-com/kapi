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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.testing.fake

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * What one call of [FakeTokenSource.refresh] does, in the order a test scripts it.
 *
 * @since 1.0.0
 */
public sealed interface RefreshOutcome {
    /**
     * The source replaces its token with [token] and reports a successful refresh.
     *
     * @property token the new token, which [toString] never shows.
     * @since 1.0.0
     */
    public class Rotate(public val token: String) : RefreshOutcome {
        override fun equals(other: Any?): Boolean = other is Rotate && token == other.token

        override fun hashCode(): Int = token.hashCode()

        override fun toString(): String = "Rotate(${SafeText.REDACTED})"
    }

    /**
     * The source keeps its token and reports a successful refresh, as a source does whose token another caller has
     * refreshed already.
     *
     * @since 1.0.0
     */
    public object Keep : RefreshOutcome {
        override fun toString(): String = "Keep"
    }

    /**
     * The source keeps its token and reports that it could not refresh it.
     *
     * @since 1.0.0
     */
    public object Refuse : RefreshOutcome {
        override fun toString(): String = "Refuse"
    }

    /**
     * The source throws an [IllegalStateException] with [message], as a broken source would.
     *
     * @property message the message of the exception.
     * @since 1.0.0
     */
    public class Fail(public val message: String) : RefreshOutcome {
        override fun equals(other: Any?): Boolean = other is Fail && message == other.message

        override fun hashCode(): Int = message.hashCode()

        override fun toString(): String = "Fail(${SafeText.quoted(message)})"
    }
}
