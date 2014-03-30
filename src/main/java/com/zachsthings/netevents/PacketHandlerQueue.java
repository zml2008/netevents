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

import com.zachsthings.netevents.packet.Packet;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

/**
 * Queue that brings packet handlers back on the main server thread
 */
class PacketHandlerQueue implements Runnable {
    private static class QueueEntry {
        private final Packet pkt;
        private final Forwarder conn;

        private QueueEntry(Packet pkt, Forwarder conn) {
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

    public void queuePacket(Packet pack, Forwarder conn) {
        toProcess.add(new QueueEntry(pack, conn));
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        QueueEntry entry;
        while ((entry = toProcess.poll()) != null) {
            try {
                entry.pkt.handle(entry.conn);
                if ((System.currentTimeMillis() - startTime) > MAX_TIME && toProcess.size() < EVENT_COUNT_THRESHOLD) {
                    break;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error occurred while handling packet from " + entry.pkt + ", skipping", e);
            }
        }

    }
}
