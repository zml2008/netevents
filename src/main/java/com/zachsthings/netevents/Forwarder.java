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
    private final AtomicReference<Connection> conn = new AtomicReference<Connection>();
    private SocketAddress reconnectAddress;

    public Forwarder(NetEventsPlugin plugin) {
        this.plugin = plugin;
    }

    class ConnectionCloseListener implements Runnable {
        @Override
        public void run() {
            conn.set(null);
            plugin.removeForwarder(Forwarder.this);
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

        final Connection conn = new Connection(plugin, chan);
        if (!this.conn.compareAndSet(null, conn)) { // Already been connected
            conn.close();
        } else {
            conn.addCloseListener(new ConnectionCloseListener());
            reconnectAddress = null; // Clear it out just in case
        }
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
