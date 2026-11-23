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

package com.davils.kapi.runtime.testing.contract

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.violation.ViolationCode

/**
 * One way in which a recorded exchange departs from its document, located twice: in the document, by the element
 * whose rule it breaks, and in the exchange, by the side, the subject and the place within the value.
 *
 * The [message] may quote text of the exchange, which comes from the code under test; [toString] and
 * [ContractReport.render] show it through `SafeText`.
 *
 * @property code the code, one of [ContractCodes].
 * @property message what is wrong.
 * @property exchange the position of the exchange among those checked, from 0.
 * @property side the side of the exchange.
 * @property operation the method and the path template of the operation, such as `GET /pets/{id}`, or the method and
 * the path of the request when no operation matches.
 * @property subject what of the side the finding is about, such as `query parameter limit`, `body` or `status`.
 * @property location the element of the document, such as the parameter or the media type, or `null` when the
 * document has none, as for an unknown path.
 * @property instance the place within the value of [subject], such as `/name` of a body, or `null` for the whole.
 * @since 1.0.0
 */
public class ContractFinding(
    public val code: ViolationCode,
    public val message: String,
    public val exchange: Int,
    public val side: ExchangeSide,
    public val operation: String,
    public val subject: String,
    public val location: Location? = null,
    public val instance: JsonPointer? = null,
) {
    private val identity: List<Any?>
        get() = listOf(code, message, exchange, side, operation, subject, location, instance?.text)

    /**
     * Renders the finding on one line: the exchange, the side, the operation, the subject, the code, the message and
     * both locations.
     *
     * @return the line.
     * @since 1.0.0
     */
    public fun render(): String {
        val place = instance?.takeUnless(JsonPointer::isRoot)?.let { pointer -> " at ${SafeText.text(pointer.text)}" }
        val where = location?.let { found -> " (${SafeText.text(found.toString())})" }
        return "#$exchange ${side.name.lowercase()} ${SafeText.text(operation)}, ${SafeText.text(subject)}" +
            "${place.orEmpty()}: [${code.value}] ${SafeText.text(message)}${where.orEmpty()}"
    }

    override fun equals(other: Any?): Boolean = other is ContractFinding && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String = "ContractFinding(${render()})"
}
