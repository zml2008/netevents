package com.zachsthings.netevents;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Holder for a fixed server UUID field that is persistent
 */
public class ServerUUID {
	private static final Logger log = Logger.getLogger(ServerUUID.class.getCanonicalName());
    private UUID serverUid;
    private final Path storeFile;

    public ServerUUID(Path storeFile) {
        this.storeFile = storeFile;
        load();
    }

    private void load() {
        if (!Files.exists(storeFile)) {
            generateNew();
        }
        try (DataInputStream str = new DataInputStream(Files.newInputStream(storeFile))) {
            final long msb = str.readLong();
            final long lsb = str.readLong();
            serverUid = new UUID(msb, lsb);
        } catch (IOException e) {
			log.warning("Failed to read server UUID, generating new");
			generateNew();
        }
    }

    private void generateNew() {
        serverUid = UUID.randomUUID();
        try (DataOutputStream str = new DataOutputStream(Files.newOutputStream(storeFile))) {
            str.writeLong(serverUid.getMostSignificantBits());
            str.writeLong(serverUid.getLeastSignificantBits());
        } catch (IOException e) {
			log.severe("Failed to write new server UUID, may cause issues with plugins expecting persistent UUID");
        }
    }

    public synchronized UUID get() {
        if (serverUid == null) {
            load();
        }
        return serverUid;
    }
}
