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

package com.davils.kapi.spec.asyncapi.diagnostic

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.core.policy.SeverityPolicy
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kapi.spec.core.read.Findings as SharedFindings

internal class Findings(policy: SeverityPolicy, maximum: Int) :
    SharedFindings(policy, maximum, DiagnosticCodes.LIMIT_DIAGNOSTICS, CodeRules::severityOf)
