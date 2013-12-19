package com.zachsthings.netevents;

import org.bukkit.configuration.Configuration;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the immutable data from one configuration instance.
 */
public class NetEventsConfig {
    public static final int DEFAULT_PORT = 25566;
    private final SocketAddress listenAddress;
    private final List<SocketAddress> connectAddresses = new ArrayList<SocketAddress>();

    public NetEventsConfig(Configuration config) {
        listenAddress = toSocketAddr(config.getString("listen-at"));

        final List<String> forwardAddresses = config.getStringList("forward-to");
        for (String forwardTo : forwardAddresses) {
            connectAddresses.add(toSocketAddr(forwardTo));
        }
    }

    public SocketAddress getListenAddress() {
        return listenAddress;
    }

    public List<SocketAddress> getConnectAddresses() {
        return Collections.unmodifiableList(connectAddresses);
    }

    private InetSocketAddress toSocketAddr(String addr) {
        final String[] listenAddr = addr.split(":");

        return new InetSocketAddress(listenAddr[0], listenAddr.length > 1 ? Integer.parseInt(listenAddr[1]) : DEFAULT_PORT);
    }
}
