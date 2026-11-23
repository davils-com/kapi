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

package com.davils.kapi.spec.schema.format

import com.davils.kapi.runtime.core.pattern.UnicodeData
import com.davils.kore.core.annotation.DavilsInternal

internal object BidiRule {
    private val rightToLeft: Set<String> = setOf("R", "AL", "AN")
    private val firstClasses: Set<String> = setOf("L", "R", "AL")
    private val leftToRightClasses: Set<String> = setOf("L", "EN", "ES", "CS", "ET", "ON", "BN", "NSM")
    private val rightToLeftClasses: Set<String> = setOf("R", "AL", "AN", "EN", "ES", "CS", "ET", "ON", "BN", "NSM")
    private val leftToRightEnds: Set<String> = setOf("L", "EN")
    private val rightToLeftEnds: Set<String> = setOf("R", "AL", "EN", "AN")

    fun domainReason(labels: List<IntArray>): String? {
        val classes = labels.map { label -> label.map(UnicodeData::bidiClassOf) }
        val isBidiDomain = classes.any { label -> label.any { it in rightToLeft } }
        val isValid = !isBidiDomain || classes.all(::isLabelValid)
        return "a label breaks the Bidi rule of RFC 5893".takeUnless { isValid }
    }

    private fun isLabelValid(classes: List<String>): Boolean {
        val first = classes.firstOrNull() ?: return true
        val last = classes.lastOrNull { it != "NSM" }
        val isLeftToRight = first == "L"
        val isLeftToRightValid = classes.all { it in leftToRightClasses } && last in leftToRightEnds
        val hasBothNumbers = "EN" in classes && "AN" in classes
        val isRightToLeftValid = classes.all { it in rightToLeftClasses } && last in rightToLeftEnds && !hasBothNumbers
        val isDirectionValid = (isLeftToRight && isLeftToRightValid) || (!isLeftToRight && isRightToLeftValid)
        return first in firstClasses && isDirectionValid
    }
}
