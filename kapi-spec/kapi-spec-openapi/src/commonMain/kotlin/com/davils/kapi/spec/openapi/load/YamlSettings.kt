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

package com.davils.kapi.spec.openapi.load

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlDuplicateKeys
import com.davils.kaml.core.YamlUnknownTags
import com.davils.kaml.core.schema.YamlSchema
import com.davils.kaml.core.schema.yamlSchema
import com.davils.kaml.core.yaml
import com.davils.kapi.spec.openapi.policy.ReadLimits

internal object YamlSettings {
    fun yamlOf(limits: ReadLimits, hasMergeKeys: Boolean): Yaml {
        val core = yamlSchema(YamlSchema.Core) { this.hasMergeKeys = hasMergeKeys }
        return yaml {
            schema = core
            duplicateKeys = YamlDuplicateKeys.Refuse
            unknownTags = YamlUnknownTags.Refuse
            limits {
                maximumInputLength = limits.maximumDocumentLength
                maximumDepth = limits.maximumDepth
                maximumNodes = limits.maximumNodes
                maximumAliasExpansion = limits.maximumAliasExpansion
                maximumScalarLength = limits.maximumScalarLength
                maximumDocuments = 1
            }
        }
    }
}
