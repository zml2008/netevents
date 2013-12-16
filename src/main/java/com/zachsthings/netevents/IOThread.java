package com.zachsthings.netevents;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
* Thread able to handle IO operations
*/
abstract class IOThread extends Thread {
    protected final SocketChannel chan;
    protected final ByteBuffer headerBuf = ByteBuffer.allocate(1 + 4);
    protected final Selector selector;

    public IOThread(SocketChannel chan, int selectorOps) throws IOException {
        this.chan = chan;
        this.selector = Selector.open();
        //chan.register(selector, selectorOps);
    }

    @Override
    public void run() {
        try {
            while (chan.isConnected()) {
                //selector.select();
                act();
            }
        } catch (ClosedChannelException e) {
            // Channel's closed, we're done
            return;
        } catch (IOException e) {
            e.printStackTrace();
            try {
                chan.close();
            } catch (IOException e1) {
                // Squash, nothing else we can do
            }
        }
    }

    protected abstract void act() throws IOException;
}
