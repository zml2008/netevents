package com.zachsthings.netevents;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles logic of connection management and teardown
 */
class Forwarder implements Closeable {
    private final NetEventsPlugin plugin;
    private final SocketAddress connectAddress;
    private final AtomicReference<Connection> conn = new AtomicReference<Connection>();
    private final boolean reconnect;

    /**
     * @param connectAddress The address to connect to.
     * @param reconnect      Whether to reconnect. Should be true for manually-specified servers, not for autodetected ones
     */
    public Forwarder(NetEventsPlugin plugin, SocketAddress connectAddress, boolean reconnect) {
        this.plugin = plugin;
        this.connectAddress = connectAddress;
        this.reconnect = reconnect;
    }

    Forwarder(NetEventsPlugin plugin, SocketChannel chan) throws IOException {
        this(plugin, chan.getRemoteAddress(), false);
        final Connection conn = new Connection(plugin, chan);
        conn.addCloseListener(new ConnectionCloseListener());
        this.conn.set(conn);
    }

    class ConnectionCloseListener implements Runnable {
        @Override
        public void run() {
            conn.set(null);
            plugin.removeForwarder(Forwarder.this);
        }
    }

    public void connect() throws IOException {
        if (!reconnect) { // This socket can only be connected once, don't bother
            return;
        }

        close();
        final SocketChannel sock;
        try {
        sock = SocketChannel.open(connectAddress);
        } catch (UnresolvedAddressException e) {
            throw new IOException("Unknown host: " + connectAddress);
        }
        Connection.configureSocketChannel(sock);

        final Connection conn = new Connection(plugin, sock);
        conn.addCloseListener(new ConnectionCloseListener());
        if (!this.conn.compareAndSet(null, conn)) { // Already been connected
            conn.close();
        }
    }

    public void close() throws IOException {
        final Connection conn = this.conn.get();
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

    public boolean isActive() {
        return this.conn.get() != null;
    }

    public SocketAddress getRemoteAddress() {
        return connectAddress;
    }

    @Override
    public String toString() {
        return "Forwarder{" +
                "connectAddress=" + connectAddress +
                ", conn=" + conn +
                ", reconnect=" + reconnect +
                '}';
    }
}
