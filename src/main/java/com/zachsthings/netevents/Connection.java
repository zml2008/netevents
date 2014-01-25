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
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a single connection. Forwarder wraps this connection for reconnecting, but once this is closed it's closed permanently.
 */
public class Connection implements Closeable {
    private final NetEventsPlugin plugin;
    // State tracking
    private final AtomicBoolean disconnectHandled = new AtomicBoolean();
    private final List<Runnable> closeListeners = new CopyOnWriteArrayList<Runnable>();
    // Connection objects
    private final SocketChannel chan;
    private final OutputThread out;
    private final InputThread in;
    private final SocketAddress remoteAddress;

    public Connection(NetEventsPlugin plugin, SocketChannel chan) throws IOException {
        this.plugin = plugin;
        this.chan = chan;
        this.remoteAddress = chan.getRemoteAddress();
        if (remoteAddress == null) {
            System.out.println("Null remote address for " + chan);
        }
        this.out = new OutputThread(this);
        this.in = new InputThread(this, plugin);
        out.start();
        in.start();
    }

    public void close() throws IOException {
        chan.close();
        handleClosed();
    }

    void handleClosed() {
        if (chan.isConnected()) {
            return;
        }
        out.interrupt();
        in.interrupt();
        if (disconnectHandled.compareAndSet(false, true)) {
            for (Runnable r : closeListeners) {
                r.run();
            }
        }
    }

    public void write(Packet p) {
        if (!chan.isConnected()) {
            throw new IllegalStateException("Channel not connected");
        }
        out.sendQueue.addLast(p);
    }

    SocketChannel getChannel() {
        return chan;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void addCloseListener(Runnable listener) {
        closeListeners.add(listener);
    }

    public NetEventsPlugin getPlugin() {
        return plugin;
    }

    public static class OutputThread extends IOThread {
        private final BlockingDeque<Packet> sendQueue = new LinkedBlockingDeque<Packet>();

        public OutputThread(Connection conn) throws IOException {
            super(conn);
        }

        @Override
        public void act() throws IOException {
            Packet packet;
            try {
                while ((packet = sendQueue.takeFirst()) != null) {
                    ByteBuffer payload = packet.write();
                    headerBuf.clear();
                    headerBuf.put(packet.getOpcode());
                    headerBuf.putInt(payload.limit());

                    headerBuf.flip();
                    payload.flip();
                    chan.write(headerBuf);
                    int written = 0;
                    while (written < payload.limit()) {
                        written += chan.write(payload);
                    }
                }
            } catch (InterruptedException e) {
                conn.close();
            }
        }
    }

    public static class InputThread extends IOThread {
        private final NetEventsPlugin plugin;
        public InputThread(Connection conn, NetEventsPlugin plugin) throws IOException {
            super(conn);
            this.plugin = plugin;
        }

        @Override
        public void act() throws IOException {
            headerBuf.clear();
            int read = chan.read(headerBuf);
            if (read == -1) {
                throw new ClosedChannelException();
            }
            headerBuf.flip();

            final int opcode = headerBuf.get();
            final int len = headerBuf.getInt();

            ByteBuffer payload = ByteBuffer.allocate(len);
            read = 0;
            while (read < len) {
                int tempRead = chan.read(payload);
                if (tempRead == -1) {
                    throw new ClosedChannelException();
                }
                read += tempRead;
            }
            payload.flip();

            Packet packet;
            switch (opcode) {
                case Opcodes.PASS_EVENT:
                    packet = EventPacket.read(payload);
                    if (packet == null) {
                        plugin.debug("Unknown event received from " + conn.getRemoteAddress());
                     }
                    break;
                default:
                    throw new IOException("Unknown opcode " + opcode + " received");
            }
            if (packet != null) {
                plugin.debug("Received packet " + packet + " from " + conn.getRemoteAddress());
                plugin.getHandlerQueue().queuePacket(packet, conn);
            }
        }
    }

    static void configureSocketChannel(SocketChannel chan) throws IOException {
        chan.configureBlocking(true);
        chan.setOption(StandardSocketOptions.TCP_NODELAY, true);
    }
}
