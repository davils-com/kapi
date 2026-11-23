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

package com.davils.kapi.runtime.core.header

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The grammar of HTTP fields of RFC 9110 §5, shared by the runtimes so that every module checks names and values
 * the same way.
 *
 * Internal to kapi: the client and the server runtime check the names and values they write and read with it. Every
 * check is a hand-written scan in time linear in the length of its input. The object keeps no state and is safe to use
 * from any thread.
 *
 * @since 1.0.0
 */
@DavilsInternal
public object FieldSyntax {
    /**
     * Returns whether [text] is a `token` of RFC 9110 §5.6.2, as a field name or a method is.
     *
     * @param text the text, which may come from an attacker.
     * @return `true` if [text] is not empty and holds only token characters.
     * @since 1.0.0
     */
    public fun isToken(text: String): Boolean = FieldGrammar.isToken(text)

    /**
     * Returns whether [char] may appear in a `token` of RFC 9110 §5.6.2.
     *
     * @param char the character.
     * @return `true` for a letter or a digit of ASCII and the symbols RFC 9110 allows in a token.
     * @since 1.0.0
     */
    public fun isTokenChar(char: Char): Boolean = FieldGrammar.isTokenChar(char)

    /**
     * Returns whether [char] is a control character a field value must not hold: every character below the space but
     * the tab, and DEL.
     *
     * @param char the character.
     * @return `true` for a control character, a line break included.
     * @since 1.0.0
     */
    public fun isControl(char: Char): Boolean = FieldGrammar.isControl(char)

    /**
     * Returns whether [text] may be the value of a field line: it holds no control character but the tab, so it
     * cannot break the line or inject another field. Characters beyond ASCII are allowed, as `obs-text` is.
     *
     * @param text the value, which may come from an attacker.
     * @return `true` if [text] holds no control character other than a tab.
     * @since 1.0.0
     */
    public fun isFieldValue(text: String): Boolean = text.none(FieldGrammar::isControl)
}
