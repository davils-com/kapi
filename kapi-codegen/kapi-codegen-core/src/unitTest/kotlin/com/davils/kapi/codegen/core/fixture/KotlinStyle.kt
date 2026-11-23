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

package com.davils.kapi.codegen.core.fixture

import com.davils.kapi.codegen.core.GeneratedFile
import com.davils.kapi.codegen.core.GeneratedPlatform
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens

private const val COMMENT: String = "a comment"

private const val LATEINIT: String = "lateinit"

private const val ASSERTION: String = "!!"

private const val EXCLAMATION: String = "!"

private const val IMPORT: String = "import"

private const val MARKER: String = "pwned"

private const val RAW: String = "\"\"\""

private val forbidden: Map<Any, String> = mapOf(
    KtTokens.EOL_COMMENT to COMMENT,
    KtTokens.BLOCK_COMMENT to COMMENT,
    KtTokens.SHEBANG_COMMENT to COMMENT,
    KtTokens.ELSE_KEYWORD to "else",
    KtTokens.EXCLEXCL to ASSERTION,
)

private val declarations: Set<Any> = setOf(KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD, KtTokens.OBJECT_KEYWORD)

private val platformOnly: List<String> = listOf(
    "java.",
    "javax.",
    "io.ktor.server.",
    "com.davils.kapi.runtime.server.",
)

internal object KotlinStyle {
    fun problems(files: List<GeneratedFile>): List<String> = Golden.kotlin(files).flatMap(::problems)

    private fun problems(file: GeneratedFile): List<String> {
        val tokens = tokens(file.content)
        val pairs = tokens.zipWithNext()
        val found = tokens.mapNotNull { token -> forbidden[token.type] }
        val asserted = pairs.filter { (first, second) -> first.text == EXCLAMATION && second.text == EXCLAMATION }
        val late = tokens.filter { token -> token.type == KtTokens.IDENTIFIER && token.text == LATEINIT }
        val declared = pairs.count { (before, token) -> isTopLevelType(before, token) }
        val types = listOf("$declared top-level types").filter { declared != 1 }
        val all = found + asserted.map { ASSERTION } + late.map { LATEINIT } + types + platform(file)
        return all.map { problem -> "${file.path}: $problem" }
    }

    fun injections(file: GeneratedFile): List<String> {
        val tokens = tokens(file.content)
        val pairs = tokens.zipWithNext()
        val calls = pairs.filter { (first, second) -> first.text == MARKER && second.type == KtTokens.LPAR }
        val declared = pairs.filter { (first, second) -> first.type == KtTokens.FUN_KEYWORD && second.text == MARKER }
        val raw = tokens.filter { token -> token.type == KtTokens.OPEN_QUOTE && token.text == RAW }
        val found =
            calls.map { "a call of $MARKER" } + declared.map { "a function $MARKER" } + raw.map { "a raw string" }
        return found.map { problem -> "${file.path}: $problem" }
    }

    private fun platform(file: GeneratedFile): List<String> {
        if (file.platform != GeneratedPlatform.Common) return emptyList()
        val imports = file.content.lines().filter { line -> line.startsWith(IMPORT) }
        val named = imports.map { line -> line.removePrefix(IMPORT).trim() }
        val jvm = named.filter { name -> platformOnly.any { prefix -> name.startsWith(prefix) } }
        return jvm.map { name -> "imports $name in common code" }
    }

    private fun isTopLevelType(before: StyleToken, token: StyleToken): Boolean {
        val isDeclaration = token.depth == 0 && token.type in declarations
        return isDeclaration && before.type != KtTokens.COLONCOLON && before.type != KtTokens.COMPANION_KEYWORD
    }

    private fun tokens(content: String): List<StyleToken> {
        val lexer = KotlinLexer()
        lexer.start(content)
        val tokens = mutableListOf<StyleToken>()
        var depth = 0
        while (lexer.tokenType != null) {
            if (lexer.tokenType == KtTokens.RBRACE) depth -= 1
            if (lexer.tokenType != KtTokens.WHITE_SPACE) tokens += StyleToken(lexer.tokenType, lexer.tokenText, depth)
            if (lexer.tokenType == KtTokens.LBRACE) depth += 1
            lexer.advance()
        }
        return tokens
    }
}
