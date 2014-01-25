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
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles logic of connection management and teardown.
 *
 * Wraps connection a lot, but handles the logic of state tracking in {@link com.zachsthings.netevents.NetEventsPlugin} and autoreconnect.
 */
class Forwarder implements Closeable {
    private final NetEventsPlugin plugin;
    private final AtomicReference<Connection> conn = new AtomicReference<>();
    private SocketAddress reconnectAddress;

    public Forwarder(NetEventsPlugin plugin) {
        this.plugin = plugin;
    }

    class ConnectionCloseListener implements Runnable {
        @Override
        public void run() {
            conn.set(null);
            if (Forwarder.this.reconnectAddress != null) {
                plugin.getReconnectTask().schedule(Forwarder.this);
            } else {
                plugin.removeForwarder(Forwarder.this);
            }
        }
    }

    public void connect(SocketAddress addr) throws IOException {
        reconnectAddress = addr;
        final SocketChannel sock;
        try {
            sock = SocketChannel.open(addr);
        } catch (UnresolvedAddressException e) {
            throw new IOException("Unknown host: " + addr);
        }
        connect(sock);
        reconnectAddress = addr;
    }

    public void connect(SocketChannel chan) throws IOException {
        Connection.configureSocketChannel(chan);
        chan = plugin.getSocketWrapper().wrapSocket(chan);

        final Connection conn = new Connection(plugin, chan);
        if (!this.conn.compareAndSet(null, conn)) { // Already been connected
            conn.close();
        } else {
            conn.addCloseListener(new ConnectionCloseListener());
            reconnectAddress = null; // Clear it out in case of previous connection
            plugin.debug("Connected to " + chan.getRemoteAddress());
        }
    }

    public boolean isReconnectable() {
        return this.reconnectAddress != null;
    }

    boolean reconnect() throws IOException {
        SocketAddress reconnectAddress = this.reconnectAddress;
        if (reconnectAddress != null) {
            connect(reconnectAddress);
            return true;
        }
        return false;
    }

    public void close() throws IOException {
        final Connection conn = this.conn.get();
        if (conn != null) {
            conn.close();
        }
        reconnectAddress = null;
    }

    public void write(Packet packet) {
        final Connection conn = this.conn.get();
        if (conn != null) {
            conn.write(packet);
        }
    }

    public boolean isActive() {
        return this.conn.get() != null;
    }

    public SocketAddress getRemoteAddress() {
        final Connection conn = this.conn.get();
        if (conn != null) {
            return conn.getRemoteAddress();
        } else {
            return reconnectAddress;
        }
    }

    @Override
    public String toString() {
        return "Forwarder{" +
                "conn=" + conn +
                ", reconnectAddress=" + reconnectAddress +
                '}';
    }
}
