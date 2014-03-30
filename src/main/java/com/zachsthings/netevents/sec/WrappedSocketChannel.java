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
package com.zachsthings.netevents.sec;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
* Wrapped socket channel.
 * Exists so additional functionality can be implemented in subclasses.
*/
public class WrappedSocketChannel extends SocketChannel {
    private final SocketChannel wrappee;

    public WrappedSocketChannel(SocketChannel wrappee) {
        super(wrappee.provider());
        this.wrappee = wrappee;
    }

    @Override
    public SocketChannel bind(SocketAddress local) throws IOException {
        return wrappee.bind(local);
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return wrappee.getLocalAddress();
    }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        return wrappee.setOption(name, value);
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return wrappee.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return wrappee.supportedOptions();
    }

    @Override
    public SocketChannel shutdownInput() throws IOException {
        return wrappee.shutdownInput();
    }

    @Override
    public SocketChannel shutdownOutput() throws IOException {
        return wrappee.shutdownOutput();
    }

    @Override
    public Socket socket() {
        return wrappee.socket();
    }

    @Override
    public boolean isConnected() {
        return wrappee.isConnected();
    }

    @Override
    public boolean isConnectionPending() {
        return wrappee.isConnectionPending();
    }

    @Override
    public boolean connect(SocketAddress remote) throws IOException {
        return wrappee.connect(remote);
    }

    @Override
    public boolean finishConnect() throws IOException {
        return wrappee.finishConnect();
    }

    @Override
    public SocketAddress getRemoteAddress() throws IOException {
        return wrappee.getRemoteAddress();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return wrappee.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        return wrappee.read(dsts, offset, length);
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return wrappee.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        return wrappee.write(srcs, offset, length);
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
        wrappee.close();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "wrappee=" + wrappee +
                '}';
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        wrappee.configureBlocking(block);
    }
}
