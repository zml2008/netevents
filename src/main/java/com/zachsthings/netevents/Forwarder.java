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

import com.zachsthings.netevents.packet.DisconnectPacket;
import com.zachsthings.netevents.packet.Packet;
import com.zachsthings.netevents.packet.ServerIDPacket;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles logic of connection management and teardown.
 *
 * Wraps connection a lot, but handles the logic of state tracking in {@link com.zachsthings.netevents.NetEventsPlugin} and autoreconnect.
 */
public class Forwarder implements Closeable {
    private final NetEventsPlugin plugin;
    private final AtomicReference<Connection> conn = new AtomicReference<>();
    private SocketAddress reconnectAddress;
    private final AtomicReference<UUID> remoteServerUUID = new AtomicReference<>();

    public Forwarder(NetEventsPlugin plugin) {
        this.plugin = plugin;
    }

    class ConnectionCloseListener implements Runnable {
        @Override
        public void run() {
            if (Forwarder.this.reconnectAddress != null) {
                plugin.getReconnectTask().schedule(Forwarder.this);
            } else {
                plugin.removeForwarder(Forwarder.this);
            }
            conn.set(null);
        }
    }

    public void connect(SocketAddress addr) throws IOException {
        remoteServerUUID.set(null);
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

        final Connection conn = Connection.open(this, chan);
        if (!this.conn.compareAndSet(null, conn)) { // Already been connected
            conn.close();
        } else {
            // Successfully connected, now perform initialization
            conn.addCloseListener(new ConnectionCloseListener());
            reconnectAddress = null; // Clear it out in case of previous connection
            write(new ServerIDPacket(plugin.getServerUUID()));
            plugin.debug("Connected to " + chan.getRemoteAddress());
        }
    }

    public boolean isReconnectable() {
        return this.reconnectAddress != null;
    }

    public UUID getRemoteServerUUID() {
        return remoteServerUUID.get();
    }

    public void setRemoteServerUUID(UUID remoteUid) {
        if (!remoteServerUUID.compareAndSet(null, remoteUid)) {
            throw new IllegalStateException("Server UUID has already been set for " + this);
        }
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
        reconnectAddress = null;
        if (conn != null) {
            conn.close();
        }
    }

    public void write(Packet packet) {
        final Connection conn = this.conn.get();
        if (conn != null) {
            conn.write(packet);
        }
    }

    public void disconnect(String reason) throws IOException {
        final Connection conn = this.conn.get();
        if (conn != null) {
            plugin.getLogger().info("Disconnecting from " + conn + ": " + reason);
            conn.writeAndClose(new DisconnectPacket(reason, false));
        }
    }

    public NetEventsPlugin getPlugin() {
        return plugin;
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
