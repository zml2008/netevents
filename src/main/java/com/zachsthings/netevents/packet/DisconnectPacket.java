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

/**
 * Packet sent to notify other side of being disconnected
 */
public class DisconnectPacket implements Packet {
    private final String disconnectMessage;
    private final boolean tryReconnect;

    public DisconnectPacket(String disconnectMessage, boolean tryReconnect) {
        this.disconnectMessage = disconnectMessage;
        this.tryReconnect = tryReconnect;
    }


    @Override
    public byte getOpcode() {
        return Opcodes.DISCONNECT;
    }

    @Override
    public void handle(Forwarder session) {
        session.getPlugin().getLogger().info("Disconnected from " + session + ": " + disconnectMessage);
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DisconnectPacket read(ByteBuffer buf) throws IOException {
        byte[] stringBytes = new byte[buf.getShort()];
        buf.get(stringBytes);

        String disconnectMessage = new String(stringBytes, "utf-8");
        final boolean tryReconnect = buf.get() == 1;
        return new DisconnectPacket(disconnectMessage, tryReconnect);

    }

    @Override
    public ByteBuffer write() throws IOException {
        byte[] stringBytes = disconnectMessage.getBytes("utf-8");
        ByteBuffer buf = ByteBuffer.allocate(2 + stringBytes.length + 1);

        buf.putShort((short) stringBytes.length);
        buf.put(stringBytes);
        buf.put(tryReconnect ? (byte) 1 : 0);

        return buf;
    }
}
