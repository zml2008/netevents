package com.zachsthings.netevents.conn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author zml2008
 */
public class RemoteConnection {
    private final InetSocketAddress connectAddress;
    private Socket sock;
    private final boolean reconnect;

    /**
     *
     * @param connectAddress The address to connect to.
     * @param reconnect Whether to reconnect. Should be true for manually-specified servers, not for autodetected ones
     */
    public RemoteConnection(InetSocketAddress connectAddress, boolean reconnect) {
        this.connectAddress = connectAddress;
        this.reconnect = reconnect;
    }

    public synchronized void connect() throws IOException {
        disconnect();
        sock = new Socket();
        sock.connect(connectAddress);
        OutputThread output = new OutputThread(sock.getOutputStream());
        InputThread input = new InputThread(sock.getInputStream());
        input.run();
        output.run();
    }

    public synchronized void disconnect() throws IOException {
        if (sock != null) {
            sock.close();
            sock = null;
        }
    }

    public static class OutputThread extends Thread {
        private final OutputStream output;

        public OutputThread(OutputStream output) {
            this.output = output;
        }

        @Override
        public void run() {

        }
    }

    public static class InputThread extends Thread {
        private final InputStream input;

        public InputThread(InputStream input) {
            this.input = input;
        }

        @Override
        public void run() {

        }
    }
}
