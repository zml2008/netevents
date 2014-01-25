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

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Receives new clients for usage
 */
class Receiver implements Closeable {
    private final NetEventsPlugin plugin;
    private final SocketAddress bindAddress;
    private ServerSocketChannel server;

    public Receiver(NetEventsPlugin plugin, SocketAddress bindAddress) {
        this.plugin = plugin;
        this.bindAddress = bindAddress;
    }

    public void bind() throws IOException {
        server = ServerSocketChannel.open();
        server.configureBlocking(true);
        server.bind(this.bindAddress);
        new ListenThread().start();
    }

    @Override
    public void close() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    public SocketAddress getBoundAddress() {
        return bindAddress;
    }

    private class ListenThread extends Thread {
        public ListenThread() {
            super("NetEvents-Listener");
        }
        @Override
        public void run() {
            try {
                while (server.isOpen()) {
                    SocketChannel client = server.accept();
                    plugin.debug("Received connection from " + client.getRemoteAddress());
                    Connection.configureSocketChannel(client);
                    Forwarder forward = new Forwarder(plugin);
                    forward.connect(client);
                    plugin.addForwarder(forward);
                }
            } catch (ClosedChannelException ignore) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
