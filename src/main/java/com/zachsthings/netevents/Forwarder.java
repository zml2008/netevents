package com.zachsthings.netevents;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zml2008
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
        conn.set(new Connection(plugin, chan));
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
        if (!this.conn.compareAndSet(null, conn)) { // Already been reconnected
            conn.close();
        }
    }

    public void close() throws IOException {
        final Connection conn = this.conn.getAndSet(null);
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
