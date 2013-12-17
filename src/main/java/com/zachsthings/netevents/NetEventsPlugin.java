package com.zachsthings.netevents;

import com.zachsthings.netevents.test.TestEvent;
import com.zachsthings.netevents.test.TestListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.logging.Level;

public class NetEventsPlugin extends JavaPlugin {
    // Number of event UUID's to keep to prevent duplicate events. Greater number potentially decreases duplicate events received
    public static final int EVENT_CACHE_COUNT = 5000;
    public static final int DEFAULT_PORT = 25566;

    private final LinkedList<UUID> processedEvents = new LinkedList<UUID>();
    private final Map<SocketAddress, Forwarder> forwarders = new HashMap<SocketAddress, Forwarder>();
    private Receiver receiver;
    private PacketHandlerQueue handlerQueue;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        handlerQueue = new PacketHandlerQueue(this);
        handlerQueue.schedule();
        reloadConfig();
        try {
            connect();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while connecting to remote servers. Are your addresses entered correctly?", e);
            getPluginLoader().disablePlugin(this);
            return;
        }
        getCommand("netevents").setExecutor(new StatusCommand(this));

        getServer().getPluginManager().registerEvents(new TestListener(), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        callEvent(new TestEvent());
        sender.sendMessage(ChatColor.BLUE + "Called event");
        return true;
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

    private void close() throws IOException {
       if (receiver != null) {
            receiver.close();
        }
        for (Iterator<Forwarder> it = forwarders.values().iterator(); it.hasNext();) {
            Forwarder conn = it.next();
            it.remove();
            conn.close();
        }
    }

    private void connect() throws IOException {
        if (receiver != null) {
            receiver.bind();
        }
        for (Forwarder conn : forwarders.values()) {
            try {
                conn.connect();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, "Unable to connect to remote server " + conn.getRemoteAddress(), ex);
            }
        }
    }

    PacketHandlerQueue getHandlerQueue() {
        return handlerQueue;
    }

    void addForwarder(Forwarder forwarder) {
        forwarders.put(forwarder.getRemoteAddress(), forwarder);
    }

    void removeForwarder(Forwarder f) {
        forwarders.remove(f.getRemoteAddress());
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        final InetSocketAddress addr = toSocketAddr(getConfig().getString("listen-at"));
        receiver = new Receiver(this, addr);

        final List<String> forwardAddresses = getConfig().getStringList("forward-to");
        for (String forwardTo : forwardAddresses) {
            addForwarder(new Forwarder(this, toSocketAddr(forwardTo), true));
        }
    }

    public void reload() throws IOException {
        close();
        reloadConfig();
        connect();
    }

    private InetSocketAddress toSocketAddr(String addr) {
        final String[] listenAddr = addr.split(":");

        return new InetSocketAddress(listenAddr[0], listenAddr.length > 1 ? Integer.parseInt(listenAddr[1]) : DEFAULT_PORT);
    }

    public <T extends Event & Serializable> T callEvent(T event) {
        callEvent(new EventPacket(event), null);
        return event;
    }

    void callEvent(EventPacket packet, Connection ignoreTo) {
        while (processedEvents.size() > EVENT_CACHE_COUNT) {
            processedEvents.removeLast();
        }
        if (processedEvents.contains(packet.getUid())) {
            return;
        }
        processedEvents.add(packet.getUid());
        getServer().getPluginManager().callEvent(packet.getSendEvent());

        for (Forwarder f : forwarders.values()) {
            try {
                if (f.getRemoteAddress().equals(ignoreTo.getChannel().getRemoteAddress())) {
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            f.write(packet);
        }
    }

    Collection<Forwarder> getForwarders() {
        return forwarders.values();
    }

    public SocketAddress getBoundAddress() {
        return receiver.getBoundAddress();
    }
}
