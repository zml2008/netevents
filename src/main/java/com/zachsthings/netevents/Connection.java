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

import com.zachsthings.netevents.packet.*;

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
import java.util.logging.Level;

/**
 * Represents a single connection. {@link Forwarder} wraps connection for reconnecting, but once this is closed it's closed permanently.
 */
class Connection implements Closeable {
    // State tracking
    private final AtomicBoolean disconnectHandled = new AtomicBoolean();
    private final List<Runnable> closeListeners = new CopyOnWriteArrayList<>();
    // Connection objects
    private final SocketChannel chan;
    private OutputThread out;
    private InputThread in;
    private final SocketAddress remoteAddress;
    private final Forwarder attachment;

    private Connection(Forwarder attachment, SocketChannel chan) throws IOException {
        this.attachment = attachment;
        this.chan = chan;
        this.remoteAddress = chan.getRemoteAddress();
        if (remoteAddress == null) {
            throw new IOException("Null remote address for " + chan);
        }
    }

    private void startThreads() throws IOException {
        this.out = new OutputThread();
        this.in = new InputThread();
        out.start();
        in.start();
    }

    public static Connection open(Forwarder attachment, SocketChannel chan) throws IOException {
        final Connection ret = new Connection(attachment, chan);
        ret.startThreads();
        return ret;
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
        out.sendQueue.addLast(new PacketEntry(p, false));
    }

    public void writeAndClose(Packet p) {
        if (!chan.isConnected()) {
            // We're assuming that the channel has been disconnected from the other side,
            // so this termination packet is no longer necessary
            //throw new IllegalStateException("Channel not connected");
            return;
        }
        out.sendQueue.addLast(new PacketEntry(p, true));
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
        return attachment.getPlugin();
    }

    public Forwarder getAttachment() {
        return attachment;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "closeListeners=" + closeListeners +
                ", chan=" + chan +
                ", disconnectHandled=" + disconnectHandled +
                '}';
    }

    private static class PacketEntry {
        private final Packet packet;
        private final boolean toClose;

        private PacketEntry(Packet packet, boolean toClose) {
            this.packet = packet;
            this.toClose = toClose;
        }
    }
    public class OutputThread extends IOThread {
        private final BlockingDeque<PacketEntry> sendQueue = new LinkedBlockingDeque<>();

        public OutputThread() throws IOException {
            super("output", Connection.this);
        }

        @Override
        public void act() throws IOException {
            PacketEntry packet;
            try {
                while ((packet = sendQueue.takeFirst()) != null) {
                    write(packet.packet);
                    if (packet.toClose) {
                        conn.close();
                    }
                }
            } catch (InterruptedException e) {
                conn.close();
            }
        }

        private void write(Packet packet) throws IOException {
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
    }

    public class InputThread extends IOThread {

        public InputThread() throws IOException {
            super("input", Connection.this);
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

            try {
                Packet packet;
                switch (opcode) {
                    case Opcodes.SERVER_ID:
                        packet = ServerIDPacket.read(payload);
                        break;
                    case Opcodes.PASS_EVENT:
                        packet = EventPacket.read(payload);
                        if (packet == null) {
                            getPlugin().debug("Unknown event received from " + conn.getRemoteAddress());
                        }
                        break;
                    case Opcodes.DISCONNECT:
                        packet = DisconnectPacket.read(payload);
                        break;
                    default:
                        throw new IOException("Unknown opcode " + opcode + " received");
                }
                if (packet != null) {
                    getPlugin().debug("Received packet " + packet + " from " + conn.getRemoteAddress());
                    getPlugin().getHandlerQueue().queuePacket(packet, attachment);
                }
            } catch (Exception e) {
                getPlugin().getLogger().log(Level.SEVERE, "Unable to read packet (id " + opcode + ") from " + conn.getRemoteAddress() + ", skipping", e);
            }
        }
    }

    static void configureSocketChannel(SocketChannel chan) throws IOException {
        chan.configureBlocking(true);
        chan.setOption(StandardSocketOptions.TCP_NODELAY, true);
    }
}
