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

import com.zachsthings.netevents.packet.EventPacket;
import com.zachsthings.netevents.sec.AESSocketWrapper;
import com.zachsthings.netevents.sec.SocketWrapper;
import com.zachsthings.netevents.ping.PingListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.util.*;
import java.util.logging.Level;

/**
 * Main class for NetEvents
 */
public class NetEventsPlugin extends JavaPlugin {
    /**
     * Number of event UUID's to keep to prevent duplicate events. Greater number potentially decreases duplicate events received.
     */
    public static final int EVENT_CACHE_COUNT = 5000;

    private final LinkedList<UUID> processedEvents = new LinkedList<>();
    private final Map<SocketAddress, Forwarder> forwarders = new HashMap<>();
    private Receiver receiver;
    private PacketHandlerQueue handlerQueue;
    private ReconnectTask reconnectTask;
    private NetEventsConfig config;
    private ServerUUID uidHolder;
    private SocketWrapper socketWrapper;
    private boolean debugMode;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        uidHolder = new ServerUUID(getDataFolder().toPath().resolve("uuid.dat"));
        handlerQueue = new PacketHandlerQueue(this);
        handlerQueue.schedule();
        reconnectTask = new ReconnectTask();
        getServer().getScheduler().runTaskTimerAsynchronously(this, reconnectTask, 0, 20);
        reloadConfig();
        if (config.getPassphrase().equals("changeme")) {
            getLogger().severe("Passphrase has not been changed from default! NetEvents will not enable until this happens");
            getPluginLoader().disablePlugin(this);
            return;
        }

        socketWrapper = new AESSocketWrapper(config.getPassphrase());
        try {
            connect();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while connecting to remote servers. Are your addresses entered correctly?", e);
            getPluginLoader().disablePlugin(this);
            return;
        }
        getCommand("netevents").setExecutor(new StatusCommand(this));

        debugMode = config.defaultDebugMode();

        getServer().getPluginManager().registerEvents(new PingListener(this), this);
    }

    @Override
    public void onDisable() {
        try {
            close();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Unable to properly disconnect network connections", e);
        }
        handlerQueue.cancel();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.config = new NetEventsConfig(getConfig());
    }

    /**
     * Reload the configuration for this plugin.
     *
     * @throws IOException When an error occurs while working with connections
     */
    public void reload() throws IOException {
        close();
        reloadConfig();
        connect();
    }

    private void close() throws IOException {
        if (receiver != null) {
            receiver.close();
            receiver = null;
        }
        for (Iterator<Forwarder> it = forwarders.values().iterator(); it.hasNext();) {
            Forwarder conn = it.next();
            it.remove();
            conn.close();
        }
    }

    private void connect() throws IOException {
        if (receiver == null) {
            receiver = new Receiver(this, config.getListenAddress());
            receiver.bind();
        }

        for (SocketAddress addr : config.getConnectAddresses()) {
            Forwarder fwd = new Forwarder(this);
            try {
                fwd.connect(addr);
            } catch (IOException ex) {
                reconnectTask.schedule(fwd);
                getLogger().log(Level.SEVERE, "Unable to connect to remote server " + addr + " (will keep trying): " + ex);
            }
            addForwarder(fwd);
        }
    }

    /**
     * Set whether debug logging is enabled.
     *
     * @see #debug(String)
     * @param debug The value to set debug logging to
     */
    public void setDebugMode(boolean debug) {
        debugMode = debug;
    }

    /**
     *
     * @return Whether or not debug logging is enabled
     */
    public boolean hasDebugMode() {
        return debugMode;
    }

    /**
     * Logs a debug message only if NetEvents has debug mode enabled ({@link #hasDebugMode()}).
     *
     * @param message The message to log
     */
    protected void debug(String message) {
        if (hasDebugMode()) {
            getLogger().warning("[DEBUG] " + message);
        }
    }

    PacketHandlerQueue getHandlerQueue() {
        return handlerQueue;
    }

    ReconnectTask getReconnectTask() {
        return reconnectTask;
    }

    void addForwarder(Forwarder forwarder) {
        forwarders.put(forwarder.getRemoteAddress(), forwarder);
    }

    void removeForwarder(Forwarder f) {
        forwarders.remove(f.getRemoteAddress());
    }

    /**
     * Returns a list of forwarders currently connected to this server.
     * @return Immutable list of currently connected forwarders
     */
    public Collection<Forwarder> getForwarders() {
        return Collections.unmodifiableCollection(forwarders.values());
    }


    /**
     * Returns a persistent unique ID for this server.
     * Useful for identifying this server in the network.
     * Persisted to {@code {@link #getDataFolder()}/uuid.dat}.
     *
     * @return a unique id for this server.
     */
    public UUID getServerUUID() {
        return uidHolder.get();
    }

    /**
     * Return the address this server is currently listening on for NetEvents connections.
     *
     * @return The listening address
     */
    public SocketAddress getBoundAddress() {
        return receiver.getBoundAddress();
    }

    /**
     * Calls the passed event on this server and forwards it to all
     * connected servers to be called remotely.
     *
     * Events must be serializable.
     * Remote servers will ignore events they do not know the class of.
     *
     * @param event The event to call
     * @param <T> The event type
     * @return The event (same as passed, just here for utility)
     */
    public <T extends Event & Serializable> T callEvent(T event) {
        callEvent(new EventPacket(event), null);
        return event;
    }

    /**
     * Internal method to allow additional flexibility from events.
     *
     * @see {@link #callEvent(org.bukkit.event.Event)} to send events
     * @param packet The event packet to send
     * @param ignoreTo The forwarder to not send this packet to. This way we avoid
     */
    public synchronized void callEvent(EventPacket packet, Forwarder ignoreTo) {
        while (processedEvents.size() > EVENT_CACHE_COUNT) {
            processedEvents.removeLast();
        }
        if (processedEvents.contains(packet.getUid())) {
            return;
        }
        processedEvents.add(packet.getUid());
        getServer().getPluginManager().callEvent(packet.getSendEvent());

        for (Forwarder f : forwarders.values()) {
            if (ignoreTo != null && ignoreTo.equals(f)) {
                continue;
            }
            f.write(packet);
        }
    }

    public SocketWrapper getSocketWrapper() {
        return socketWrapper;
    }
}
