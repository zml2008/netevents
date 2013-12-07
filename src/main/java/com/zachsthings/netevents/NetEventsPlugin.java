package com.zachsthings.netevents;

import org.bukkit.plugin.java.JavaPlugin;

public class NetEventsPlugin extends JavaPlugin {
    // Number of event UUID's to keep to prevent duplicate events. Greater number potentiallly decreases duplicate events received
    public static final int EVENT_CACHE_COUNT = 5000;
    @Override
    public void onEnable() {
        //
    }

    public <T extends RemotableEvent> T callEvent(T event) {
        getServer().getPluginManager().callEvent(event);
        return event;
    }
}
