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
	/**
	 * The default port NetEvents listens at and connects to when no other is specified
	 */
    public static final int DEFAULT_PORT = 25566;
    private final SocketAddress listenAddress;
    private final List<SocketAddress> connectAddresses = new ArrayList<>();
    private final boolean defaultDebugMode;
    private final String passphrase;

    public NetEventsConfig(Configuration config) {
        listenAddress = toSocketAddr(config.getString("listen-at"));

        final List<String> forwardAddresses = config.getStringList("forward-to");
        for (String forwardTo : forwardAddresses) {
            connectAddresses.add(toSocketAddr(forwardTo));
        }
        defaultDebugMode = config.getBoolean("debug");
        passphrase = config.getString("passphrase");
    }

	/**
	 * Return the address NetEvents should listen at for incoming connections.
	 *
	 * @return The address to listen at
	 */
    public SocketAddress getListenAddress() {
        return listenAddress;
    }

	/**
	 * Return the addresses this instance of NetEvents will attempt to connect to.
	 *
	 * @return The addresses to connect to
	 */
    public List<SocketAddress> getConnectAddresses() {
        return Collections.unmodifiableList(connectAddresses);
    }

	/**
	 * Returns whether debug mode should be enabled by default
	 *
	 * @return debug mode enabled by default?
	 */
    public boolean defaultDebugMode() {
        return defaultDebugMode;
    }

    String getPassphrase() {
        return passphrase;
    }

    private InetSocketAddress toSocketAddr(String addr) {
        final String[] listenAddr = addr.split(":");

        return new InetSocketAddress(listenAddr[0], listenAddr.length > 1 ? Integer.parseInt(listenAddr[1]) : DEFAULT_PORT);
    }
}
