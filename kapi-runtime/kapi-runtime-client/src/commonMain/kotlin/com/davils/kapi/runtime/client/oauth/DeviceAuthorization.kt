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

package com.davils.kapi.runtime.client.oauth

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.time.Duration

/**
 * What a user must do to authorize a device by RFC 8628 §3.3: visit [verificationUri] and enter [userCode].
 *
 * An authorization is immutable.
 *
 * @property userCode the code the user enters.
 * @property verificationUri the URI the user visits, always `https`.
 * @property verificationUriComplete the URI with the code in it, such as for a QR code, or `null`.
 * @property expiresIn how long the code is valid.
 * @since 1.0.0
 */
public class DeviceAuthorization internal constructor(
    public val userCode: String,
    public val verificationUri: String,
    public val verificationUriComplete: String?,
    public val expiresIn: Duration,
) {
    override fun toString(): String = "DeviceAuthorization(${SafeText.text(verificationUri)}, $expiresIn)"
}
