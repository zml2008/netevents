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
public class Receiver implements Closeable {
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
        @Override
        public void run() {
            try {
                while (server.isOpen()) {
                    SocketChannel client = server.accept();
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
