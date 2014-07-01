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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Packet used for server identification
 */
public class ServerIDPacket implements Packet {
    private final UUID serverUid;

    public ServerIDPacket(UUID serverUid) {
        this.serverUid = serverUid;
    }

    @Override
    public byte getOpcode() {
        return Opcodes.SERVER_ID;
    }

    @Override
    public void handle(Forwarder session) throws IOException {
        for (Forwarder f : session.getPlugin().getForwarders()) {
            if (f != session && serverUid.equals(f.getRemoteServerUUID())) {
                session.disconnect("This server already connected!");
                return;
            }
        }
        session.setRemoteServerUUID(serverUid);
    }

    public static ServerIDPacket read(ByteBuffer buffer) throws IOException {
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        return new ServerIDPacket(new UUID(msb, lsb));
    }

    @Override
    public ByteBuffer write() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(2 * 8);
        buffer.putLong(serverUid.getMostSignificantBits());
        buffer.putLong(serverUid.getLeastSignificantBits());
        return buffer;
    }
}
