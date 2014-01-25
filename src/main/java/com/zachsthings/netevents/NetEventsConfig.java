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
    private final boolean defaultDebugMode;

    public NetEventsConfig(Configuration config) {
        listenAddress = toSocketAddr(config.getString("listen-at"));

        final List<String> forwardAddresses = config.getStringList("forward-to");
        for (String forwardTo : forwardAddresses) {
            connectAddresses.add(toSocketAddr(forwardTo));
        }
        defaultDebugMode = config.getBoolean("debug");
    }

    public SocketAddress getListenAddress() {
        return listenAddress;
    }

    public List<SocketAddress> getConnectAddresses() {
        return Collections.unmodifiableList(connectAddresses);
    }

    public boolean defaultDebugMode() {
        return defaultDebugMode;
    }

    private InetSocketAddress toSocketAddr(String addr) {
        final String[] listenAddr = addr.split(":");

        return new InetSocketAddress(listenAddr[0], listenAddr.length > 1 ? Integer.parseInt(listenAddr[1]) : DEFAULT_PORT);
    }
}
