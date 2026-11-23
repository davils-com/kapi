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

package com.davils.kapi.runtime.server.fixture

import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.Routes
import com.davils.kapi.runtime.server.reply.reply

private const val CREATED: Int = 201

private const val PETS: String = "/pets"

internal class PetsRoutes : Routes {
    override val operations: List<Operation> = listOf(
        Operation(name = "searchPets", method = "QUERY", path = PETS) { reply(CREATED) { text("query") } },
        Operation(name = "purgePets", method = "PURGE", path = "/pets/{petId}") { request ->
            reply(CREATED) { text(request.pathParameters.getValue("petId")) }
        },
        Operation(name = "failPets", method = "GET", path = "/pets/fail") { error("secret detail of the failure") },
    )
}
