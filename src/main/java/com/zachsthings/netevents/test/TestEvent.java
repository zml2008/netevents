package com.zachsthings.netevents.test;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by zach on 12/15/13.
 */
public class TestEvent extends Event implements Serializable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String hostname;

    public TestEvent() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "<unknown>";
        }
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
