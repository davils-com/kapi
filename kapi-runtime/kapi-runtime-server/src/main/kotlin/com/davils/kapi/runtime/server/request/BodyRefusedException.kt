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

package com.davils.kapi.runtime.server.request

/**
 * Thrown by a streamed [Upload] or [MultipartUpload] when the body breaks a limit or its format while the handler
 * reads it; the server answers it with the problem of [status] and [code] instead of a failure.
 *
 * A handler lets it pass. Its message names the status only, nothing of the body.
 *
 * @property status the status of the answer, such as 413 or 400.
 * @property code the code of the problem, one of [com.davils.kapi.runtime.server.problem.ServerCodes].
 * @since 1.0.0
 */
public class BodyRefusedException internal constructor(public val status: Int, public val code: String) :
    IllegalStateException("The body of the request was refused with status $status")
