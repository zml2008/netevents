package com.zachsthings.netevents;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler task to handle autoreconnect
 */
public class ReconnectTask implements Runnable {
    private static class ReconnectItem {
        private final Forwarder reconnect;
        private int runCount = 0, countPassed = 0;

        private ReconnectItem(Forwarder reconnect) {
            this.reconnect = reconnect;
        }
    }

    private final AtomicBoolean runAllNext = new AtomicBoolean();
    private final LinkedList<ReconnectItem> taskQueue = new LinkedList<ReconnectItem>();

    @Override
    public void run() {
        final boolean runAll = runAllNext.compareAndSet(true, false);
        for (Iterator<ReconnectItem> it = taskQueue.iterator(); it.hasNext(); ) {
            ReconnectItem item = it.next();
            if (item.countPassed++ == item.runCount || runAll) {
                try {
                    item.reconnect.reconnect();
                } catch (IOException e) { // Failed to connect, go again.
                    item.runCount++;
                    item.countPassed = 0;
                }
            }
        }

    }

    public void schedule(Forwarder toReconnect) {
        taskQueue.add(new ReconnectItem(toReconnect));
    }

    public void attemptAllNext() {
        runAllNext.set(true);
    }
}
