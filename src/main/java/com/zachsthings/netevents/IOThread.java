package com.zachsthings.netevents;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
* Thread able to handle IO operations
*/
abstract class IOThread extends Thread {
    protected final Connection conn;
    protected final SocketChannel chan;
    protected final ByteBuffer headerBuf = ByteBuffer.allocateDirect(1 + 4);

    public IOThread(Connection conn) throws IOException {
        this.conn = conn;
        this.chan = conn.getChannel();
    }

    @Override
    public void run() {
        try {
            while (chan.isConnected()) {
                act();
            }
        } catch (ClosedChannelException ignore) {
        } catch (IOException e) {
            conn.getPlugin().getLogger().log(Level.SEVERE, "Error occurred while processing IO", e);
        }
        try {
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract void act() throws IOException;
}
