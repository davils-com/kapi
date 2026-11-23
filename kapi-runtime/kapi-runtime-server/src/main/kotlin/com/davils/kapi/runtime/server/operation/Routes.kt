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

package com.davils.kapi.runtime.server.operation

import com.davils.kapi.runtime.server.channel.ChannelEndpoint

/**
 * What a generated server class offers to be mounted into the routing of an application: its operations and its
 * WebSocket channels.
 *
 * An application mounts it with [mount][com.davils.kapi.runtime.server.mount], such as
 * `routing { mount(PetsRoutes(service)) }`. An implementation returns the same lists every time.
 *
 * @since 1.0.0
 */
public interface Routes {
    /**
     * The operations, each served on its method and path template.
     *
     * @since 1.0.0
     */
    public val operations: List<Operation>

    /**
     * The WebSocket channels, each served on its path; none by default.
     *
     * @since 1.0.0
     */
    public val channels: List<ChannelEndpoint<*, *>> get() = emptyList()
}
