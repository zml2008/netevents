package com.zachsthings.netevents.test;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Test listener.
 */
public class TestListener implements Listener {
    @EventHandler
    public void onTestEvent(TestEvent event) {
        System.out.println("Received test event from " + event.getHostname());
    }
}
