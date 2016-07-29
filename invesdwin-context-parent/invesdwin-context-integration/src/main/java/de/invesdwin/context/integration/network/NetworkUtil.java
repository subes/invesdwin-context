package de.invesdwin.context.integration.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import de.invesdwin.context.ContextProperties;
import de.invesdwin.context.log.error.Err;
import de.invesdwin.util.time.fdate.FTimeUnit;

@Immutable
public final class NetworkUtil {

    private NetworkUtil() {}

    /**
     * Identifies the local ip of the computer in the local network.
     * 
     * If there is no local ip, just any ip of any local interface is returned.
     * 
     * @see <a href=
     *      "http://www.java-tips.org/java-se-tips/java.net/how-to-detect-ip-address-and-name-of-host-machine-without-
     *      using-socket- p r o g r a . h t m l > S o u r c e < / a >
     */
    public static InetAddress getLocalAddress() {
        Enumeration<NetworkInterface> netInterfaces = null;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (final SocketException e) {
            throw Err.process(e);
        }

        InetAddress remoteFallback = null;
        while (netInterfaces.hasMoreElements()) {
            final NetworkInterface ni = netInterfaces.nextElement();
            final Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                final InetAddress addr = address.nextElement();
                if (!addr.isLoopbackAddress() && !(addr.getHostAddress().indexOf(":") > -1)) {
                    if (addr.isSiteLocalAddress()) {
                        return addr;
                    } else {
                        try {
                            if (addr.isReachable(
                                    ContextProperties.DEFAULT_NETWORK_TIMEOUT.intValue(FTimeUnit.MILLISECONDS))) {
                                remoteFallback = addr;
                            }
                        } catch (final IOException e) { //SUPPRESS CHECKSTYLE empty
                            //ignore
                        }
                    }
                }
            }
        }
        return remoteFallback;
    }

    public static List<InetAddress> getLocalAddresses() {
        Enumeration<NetworkInterface> netInterfaces = null;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (final SocketException e) {
            throw Err.process(e);
        }

        final List<InetAddress> localAddresses = new ArrayList<InetAddress>();
        while (netInterfaces.hasMoreElements()) {
            final NetworkInterface ni = netInterfaces.nextElement();
            final Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                final InetAddress addr = address.nextElement();
                if (!addr.isLoopbackAddress() && !(addr.getHostAddress().indexOf(":") > -1)) {
                    if (addr.isSiteLocalAddress()) {
                        localAddresses.add(addr);
                    } else {
                        try {
                            if (addr.isReachable(
                                    ContextProperties.DEFAULT_NETWORK_TIMEOUT.intValue(FTimeUnit.MILLISECONDS))) {
                                localAddresses.add(addr);
                            }
                        } catch (final IOException e) { //SUPPRESS CHECKSTYLE empty
                            //ignore
                        }
                    }
                }
            }
        }
        return localAddresses;
    }

    /**
     * Identifies the external ip of the computer in the internet.
     * 
     * @see <a href="http://stackoverflow.com/questions/2939218/getting-the-external-ip-address-in-java">Source</a>
     */
    public static InetAddress getExternalAddress() {
        try {
            return InternetCheckHelper.getExternalIp();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static boolean waitIfInternetNotAvailable() throws InterruptedException {
        return waitIfInternetNotAvailable(true);
    }

    /**
     * Returns true if the internet was down.
     */
    public static boolean waitIfInternetNotAvailable(final boolean allowCache) throws InterruptedException {
        return InternetCheckHelper.waitIfInternetNotAvailable(allowCache);
    }
}
