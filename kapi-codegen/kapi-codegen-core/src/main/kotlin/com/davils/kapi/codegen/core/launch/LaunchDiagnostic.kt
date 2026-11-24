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

package com.davils.kapi.codegen.core.launch

import com.davils.kore.core.annotation.DavilsInternal

/**
 * A diagnostic of a run, with its place in the file it was found in, relative to the root of the request.
 *
 * @property code the stable code, such as `openapi.reference.cycle`.
 * @property isError whether the diagnostic is an error; a warning or a note otherwise.
 * @property message the message, already escaped for display.
 * @property file the path of the file relative to the root, with `/` as separator, or `null`.
 * @property line the line, counted from one, or `null`.
 * @property column the column, counted from one, or `null`.
 * @property isNote whether the diagnostic is a note, which informs and never fails a build, such as a change of a
 * document that breaks nobody.
 * @since 1.0.0
 */
@DavilsInternal
public class LaunchDiagnostic(
    public val code: String,
    public val isError: Boolean,
    public val message: String,
    public val file: String?,
    public val line: Int?,
    public val column: Int?,
    public val isNote: Boolean = false,
) {
    /**
     * Renders the diagnostic as one line, such as `api/petstore.yaml:3:5: error: [code] message`.
     *
     * @return the line.
     * @since 1.0.0
     */
    public fun render(): String {
        val place = listOfNotNull(file, line, column).joinToString(":")
        val severity = ERROR.takeIf { isError } ?: NOTE.takeIf { isNote } ?: WARNING
        val head = "$severity: [$code] $message"
        return "$place: $head".takeIf { place.isNotEmpty() } ?: head
    }

    override fun equals(other: Any?): Boolean = other is LaunchDiagnostic && render() == other.render()

    override fun hashCode(): Int = render().hashCode()

    override fun toString(): String = render()

    private companion object {
        const val ERROR: String = "error"

        const val WARNING: String = "warning"

        const val NOTE: String = "note"
    }
}
