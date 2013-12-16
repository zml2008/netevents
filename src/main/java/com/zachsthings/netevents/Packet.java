package com.zachsthings.netevents;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents a packet object. Provides methods to encode and handle. Decoding is handled separately because deserialization is annoying
 */
public interface Packet {
    public byte getOpcode();
    public void handle(Connection conn);
    public ByteBuffer write() throws IOException;
}
