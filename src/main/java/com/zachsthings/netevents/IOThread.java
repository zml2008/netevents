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
package com.zachsthings.netevents;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
* Thread able to handle IO operations
 *
 * Provides some utility for IO threads
*/
abstract class IOThread extends Thread {
    protected final Connection conn;
    protected final SocketChannel chan;
    protected final ByteBuffer headerBuf = ByteBuffer.allocateDirect(1 + 4);

    public IOThread(String name, Connection conn) throws IOException {
        super("NetEvents-" + name + "-" + conn.getRemoteAddress());
        this.conn = conn;
        this.chan = conn.getChannel();
    }

    @Override
    public void run() {
        try {
            while (chan.isConnected()) {
                act();
            }
        } catch (ClosedChannelException ignore) {
        } catch (IOException e) {
            conn.getPlugin().getLogger().log(Level.SEVERE, "Error occurred while processing IO for " + conn.getRemoteAddress(), e);
        }

        try {
            conn.close();
        } catch (IOException e) {
            conn.getPlugin().getLogger().log(Level.SEVERE, "Error occurred while closing connection " + conn.getRemoteAddress(), e);
        }
    }

    protected abstract void act() throws IOException;
}
