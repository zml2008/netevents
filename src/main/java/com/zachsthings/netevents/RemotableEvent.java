package com.zachsthings.netevents;

import org.bukkit.event.Event;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.UUID;

/**
 * Represents an event that can be called across servers
 *
 * All events must implement both the zero-args and argument-containing constructors
 */
public abstract class RemotableEvent extends Event {
    private final UUID uid;

    /**
     * Constructor for events received over the network
     *
     * @param uid Event unique id
     */
    public RemotableEvent(UUID uid, DataInputStream input) {
        this.uid = uid;
    }

    /**
     * Constructor for event created locally. Generates a random unique id.
     */
    public RemotableEvent() {
        this.uid = UUID.randomUUID();
    }

    protected abstract void serialize(DataOutputStream stream);
}
