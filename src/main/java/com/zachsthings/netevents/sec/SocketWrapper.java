/**
 * Copyright (C) 2014 zml (netevents@zachsthings.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zachsthings.netevents.sec;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Allows modifying of data in SocketChannel-using systems (for things like encryption)
 */
public interface SocketWrapper {
    /**
     * Return a wrapped SocketChannel instance that sources its data from {@code chan}
     * May be the original channel passed
     *
     * @param chan The source channel
     * @return The wrapped channel
     * @throws IOException If any sort of error occurs while wrapping
     */
    public SocketChannel wrapSocket(SocketChannel chan) throws IOException;
}
