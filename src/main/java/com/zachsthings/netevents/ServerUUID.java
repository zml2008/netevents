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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Holder for a fixed server UUID field that is persistent
 */
class ServerUUID {
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
