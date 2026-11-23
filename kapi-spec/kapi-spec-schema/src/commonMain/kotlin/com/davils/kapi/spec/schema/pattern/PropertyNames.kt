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

package com.davils.kapi.spec.schema.pattern

internal object PropertyNames {
    private val categoryAliases: List<Pair<List<String>, Set<String>>> = listOf(
        listOf("C", "Other") to codes("Cc Cf Cn Co Cs"),
        listOf("Cc", "Control", "cntrl") to codes("Cc"),
        listOf("Cf", "Format") to codes("Cf"),
        listOf("Cn", "Unassigned") to codes("Cn"),
        listOf("Co", "Private_Use") to codes("Co"),
        listOf("Cs", "Surrogate") to codes("Cs"),
        listOf("L", "Letter") to codes("Ll Lm Lo Lt Lu"),
        listOf("LC", "Cased_Letter") to codes("Ll Lt Lu"),
        listOf("Ll", "Lowercase_Letter") to codes("Ll"),
        listOf("Lm", "Modifier_Letter") to codes("Lm"),
        listOf("Lo", "Other_Letter") to codes("Lo"),
        listOf("Lt", "Titlecase_Letter") to codes("Lt"),
        listOf("Lu", "Uppercase_Letter") to codes("Lu"),
        listOf("M", "Mark", "Combining_Mark") to codes("Mc Me Mn"),
        listOf("Mc", "Spacing_Mark") to codes("Mc"),
        listOf("Me", "Enclosing_Mark") to codes("Me"),
        listOf("Mn", "Nonspacing_Mark") to codes("Mn"),
        listOf("N", "Number") to codes("Nd Nl No"),
        listOf("Nd", "Decimal_Number", "digit") to codes("Nd"),
        listOf("Nl", "Letter_Number") to codes("Nl"),
        listOf("No", "Other_Number") to codes("No"),
        listOf("P", "Punctuation", "punct") to codes("Pc Pd Pe Pf Pi Po Ps"),
        listOf("Pc", "Connector_Punctuation") to codes("Pc"),
        listOf("Pd", "Dash_Punctuation") to codes("Pd"),
        listOf("Pe", "Close_Punctuation") to codes("Pe"),
        listOf("Pf", "Final_Punctuation") to codes("Pf"),
        listOf("Pi", "Initial_Punctuation") to codes("Pi"),
        listOf("Po", "Other_Punctuation") to codes("Po"),
        listOf("Ps", "Open_Punctuation") to codes("Ps"),
        listOf("S", "Symbol") to codes("Sc Sk Sm So"),
        listOf("Sc", "Currency_Symbol") to codes("Sc"),
        listOf("Sk", "Modifier_Symbol") to codes("Sk"),
        listOf("Sm", "Math_Symbol") to codes("Sm"),
        listOf("So", "Other_Symbol") to codes("So"),
        listOf("Z", "Separator") to codes("Zl Zp Zs"),
        listOf("Zl", "Line_Separator") to codes("Zl"),
        listOf("Zp", "Paragraph_Separator") to codes("Zp"),
        listOf("Zs", "Space_Separator") to codes("Zs"),
    )

    private val unsupportedBinary: Set<String> = (
        "Bidi_Control Bidi_C Bidi_Mirrored Bidi_M Case_Ignorable CI Cased Changes_When_Casefolded CWCF " +
            "Changes_When_Casemapped CWCM Changes_When_Lowercased CWL Changes_When_NFKC_Casefolded CWKCF " +
            "Changes_When_Titlecased CWT Changes_When_Uppercased CWU Dash Default_Ignorable_Code_Point DI " +
            "Deprecated Dep Diacritic Dia Emoji Emoji_Component EComp Emoji_Modifier EMod Emoji_Modifier_Base " +
            "EBase Emoji_Presentation EPres Extended_Pictographic ExtPict Extender Ext Grapheme_Base Gr_Base " +
            "Grapheme_Extend Gr_Ext IDS_Binary_Operator IDSB IDS_Trinary_Operator IDST IDS_Unary_Operator IDSU " +
            "ID_Continue IDC ID_Start IDS Join_Control Join_C Logical_Order_Exception LOE Math " +
            "Noncharacter_Code_Point NChar Pattern_Syntax Pat_Syn Pattern_White_Space Pat_WS Quotation_Mark " +
            "QMark Radical Regional_Indicator RI Sentence_Terminal STerm Soft_Dotted SD Terminal_Punctuation " +
            "Term Unified_Ideograph UIdeo Variation_Selector VS XID_Continue XIDC XID_Start XIDS"
        ).split(' ').toSet()

    private val named: Map<String, (String) -> PropertyLookup> = mapOf(
        "General_Category" to ::category,
        "gc" to ::category,
        "Script" to ::script,
        "sc" to ::script,
        "Script_Extensions" to ::scriptExtension,
        "scx" to ::scriptExtension,
    )

    fun lookup(text: String): PropertyLookup {
        val separator = text.indexOf('=')
        if (separator < 0) return lone(text)
        val lookup = named[text.substring(startIndex = 0, endIndex = separator)]
        return lookup?.invoke(text.substring(startIndex = separator + 1)) ?: PropertyLookup.Unknown
    }

    fun categoryCodes(name: String): Set<String>? {
        val entry = categoryAliases.firstOrNull { (names, _) -> name in names }
        return entry?.second
    }

    private fun codes(text: String): Set<String> {
        val codes = text.split(' ')
        return codes.toSet()
    }

    private fun lone(name: String): PropertyLookup {
        val binary = BinaryProperty.entries.firstOrNull { property -> name in property.names }
        val found = categoryCodes(name)?.let(UnicodeData::categorySet) ?: binary?.let(UnicodeData::binarySet)
        val unsupported = PropertyLookup.Unsupported.takeIf { name in unsupportedBinary }
        return found?.let(PropertyLookup::Found) ?: unsupported ?: PropertyLookup.Unknown
    }

    private fun category(value: String): PropertyLookup {
        val codes = categoryCodes(value) ?: return PropertyLookup.Unknown
        return PropertyLookup.Found(UnicodeData.categorySet(codes))
    }

    private fun script(value: String): PropertyLookup {
        val index = UnicodeData.scriptIndexOf(value) ?: return PropertyLookup.Unknown
        return PropertyLookup.Found(UnicodeData.scriptSet(index))
    }

    private fun scriptExtension(value: String): PropertyLookup {
        val isKnown = UnicodeData.scriptIndexOf(value) != null
        return PropertyLookup.Unsupported.takeIf { isKnown } ?: PropertyLookup.Unknown
    }
}
