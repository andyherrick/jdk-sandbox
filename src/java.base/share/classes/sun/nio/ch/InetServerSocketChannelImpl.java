/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.channels.AlreadyBoundException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import sun.net.NetHooks;
import sun.net.ext.ExtendedSocketOptions;

/**
 * An implementation of ServerSocketChannels
 */

class InetServerSocketChannelImpl
    extends ServerSocketChannelImpl
{
    // set true when exclusive binding is on and SO_REUSEADDR is emulated
    private boolean isReuseAddress;

    // -- End of fields protected by stateLock

    // the protocol family requested by the user, or Net.UNSPEC if not specified
    private final ProtocolFamily family;

    InetServerSocketChannelImpl(SelectorProvider sp) throws IOException {
        this(sp, Net.isIPv6Available()
                ? StandardProtocolFamily.INET6
                : StandardProtocolFamily.INET);
    }

    InetServerSocketChannelImpl(SelectorProvider sp, ProtocolFamily family)
        throws IOException
    {
        super(sp, Net.serverSocket(family, true), false);
        this.family = family;
    }

    InetServerSocketChannelImpl(SelectorProvider sp, FileDescriptor fd, boolean bound)
        throws IOException
    {
        super(sp, fd, bound);
        this.family =  Net.isIPv6Available()
                ? StandardProtocolFamily.INET6
                : StandardProtocolFamily.INET;
    }


    SocketAddress localAddressImpl(FileDescriptor fd) throws IOException {
        return Net.localAddress(fd);
    }

    @Override
    <T> boolean setOptionSpecial(SocketOption<T> name, T value) throws IOException {
        if (name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
            // SO_REUSEADDR emulated when using exclusive bind
            isReuseAddress = (Boolean) value;
            return true;
        } else {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    <T> T getOptionSpecial(SocketOption<T> name) throws IOException {
        if (name == StandardSocketOptions.SO_REUSEADDR && Net.useExclusiveBind()) {
            // SO_REUSEADDR emulated when using exclusive bind
            return (T) Boolean.valueOf(isReuseAddress);
        }
        return null;
    }

    private static class DefaultOptionsHolder {
        static final Set<SocketOption<?>> defaultOptions = defaultOptions();

        private static Set<SocketOption<?>> defaultOptions() {
            HashSet<SocketOption<?>> set = new HashSet<>();
            set.add(StandardSocketOptions.SO_RCVBUF);
            set.add(StandardSocketOptions.SO_REUSEADDR);
            if (Net.isReusePortAvailable()) {
                set.add(StandardSocketOptions.SO_REUSEPORT);
            }
            set.addAll(ExtendedSocketOptions.serverSocketOptions());
            return Collections.unmodifiableSet(set);
        }
    }

    @Override
    public final Set<SocketOption<?>> supportedOptions() {
        return DefaultOptionsHolder.defaultOptions;
    }

    private static InetAddress anyLocalInet4 = Net.anyLocalInet4Address();
    private static InetAddress anyLocalInet6 = Net.anyLocalInet6Address();

    private InetSocketAddress anyLocalSocketAddress() {
        if (family == Net.UNSPEC) {
            return new InetSocketAddress(0);
        } else if (family == StandardProtocolFamily.INET) {
            return new InetSocketAddress(anyLocalInet4, 0);
        } else {
            return new InetSocketAddress(anyLocalInet6, 0);
        }
    }

    @Override
    SocketAddress bindImpl(SocketAddress local, int backlog) throws IOException {
        InetSocketAddress isa = (local == null)
            ? anyLocalSocketAddress()
            : Net.checkAddress(local, family);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkListen(isa.getPort());
        NetHooks.beforeTcpBind(getFD(), isa.getAddress(), isa.getPort());
        Net.bind(family, getFD(), isa.getAddress(), isa.getPort());
        Net.listen(getFD(), backlog < 1 ? 50 : backlog);
        return Net.localAddress(getFD());
    }

    protected int acceptImpl(FileDescriptor fd, FileDescriptor newfd, SocketAddress[] addrs)
        throws IOException
    {
        InetSocketAddress[] a = new InetSocketAddress[1];
        int n = Net.accept(fd, newfd, a);
        addrs[0] = a[0];
        return n;
    }

    protected SocketAddress getRevealedLocalAddress(SocketAddress addr) {
        return Net.getRevealedLocalAddress((InetSocketAddress)addr);
    }

    protected String getRevealedLocalAddressAsString(SocketAddress addr) {
        return Net.getRevealedLocalAddressAsString((InetSocketAddress)addr);
    }


    protected SocketChannel finishAcceptImpl(FileDescriptor newfd, SocketAddress sa)
        throws IOException
    {
        InetSocketAddress isa = (InetSocketAddress)sa;
        // check permitted to accept connections from the remote address
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkAccept(isa.getAddress().getHostAddress(), isa.getPort());
        }
        return new InetSocketChannelImpl(provider(), newfd, isa);
    }
}