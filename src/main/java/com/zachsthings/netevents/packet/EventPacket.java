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
package com.zachsthings.netevents.packet;

import com.zachsthings.netevents.Forwarder;
import org.bukkit.event.Event;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Represents an event that can be called across servers
 *
 * All events must implement both the zero-args and argument-containing constructors
 */
public class EventPacket implements Packet {
    private final UUID uid;
    private final Event sendEvent;

    /**
     * Constructor for events received over the network
     *
     * @param uid Event unique id
     */
    public EventPacket(UUID uid, Event sendEvent) {
        this.uid = uid;
        this.sendEvent = sendEvent;
    }

    public EventPacket(Event sendEvent) {
        this(UUID.randomUUID(), sendEvent);
    }

    public UUID getUid() {
        return uid;
    }

    public Event getSendEvent() {
        return sendEvent;
    }

    public static EventPacket read(ByteBuffer buf) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining()));
        final long msb = ois.readLong(), lsb = ois.readLong();
        final UUID uid = new UUID(msb, lsb);

        Object o;
        try {
            o = ois.readObject();
        } catch (ClassNotFoundException e) {
            return null;
        }

        if (!(o instanceof Event)) {
            throw new IOException("Read object " + o + " is not an Event");
        }
        return new EventPacket(uid, (Event) o);
    }

    @Override
    public byte getOpcode() {
        return Opcodes.PASS_EVENT;
    }

    @Override
    public void handle(Forwarder forwarder) {
        forwarder.getPlugin().callEvent(this, forwarder);
    }

    public ByteBuffer write() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeLong(uid.getMostSignificantBits());
        oos.writeLong(uid.getLeastSignificantBits());

        oos.writeObject(sendEvent);
        oos.flush();
        oos.close();

        ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());
        buf.position(buf.limit());
        return buf;
    }

    @Override
    public String toString() {
        return "EventPacket{" +
                "uid=" + uid +
                ", sendEvent=" + sendEvent +
                '}';
    }
}
