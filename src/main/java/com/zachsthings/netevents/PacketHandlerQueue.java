package com.zachsthings.netevents;

import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Queue that brings packet handlers back on the main server thread
 */
public class PacketHandlerQueue implements Runnable {
    private static class QueueEntry {
        private final Packet pkt;
        private final Connection conn;

        private QueueEntry(Packet pkt, Connection conn) {
            this.pkt = pkt;
            this.conn = conn;
        }
    }

    public static final int MAX_TIME = 25, EVENT_COUNT_THRESHOLD = 10;

    private final Queue<QueueEntry> toProcess = new LinkedList<QueueEntry>();
    private final NetEventsPlugin plugin;
    private BukkitTask task = null;

    public PacketHandlerQueue(NetEventsPlugin plugin) {
        this.plugin = plugin;
    }

    public void schedule() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 0, 1);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }

    public void queuePacket(Packet pack, Connection conn) {
        toProcess.add(new QueueEntry(pack, conn));
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        QueueEntry entry;
        while ((entry = toProcess.poll()) != null) {
            entry.pkt.handle(entry.conn);
            if ((System.currentTimeMillis() - startTime) > MAX_TIME && toProcess.size() < EVENT_COUNT_THRESHOLD) {
                break;
            }
        }

    }
}
