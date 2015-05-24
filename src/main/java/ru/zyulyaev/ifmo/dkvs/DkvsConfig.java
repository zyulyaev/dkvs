package ru.zyulyaev.ifmo.dkvs;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by nikita on 16.05.15.
 */
public class DkvsConfig {
    private final List<SocketAddress> addresses;
    private final int timeout;

    public DkvsConfig(Collection<? extends SocketAddress> addresses, int timeout) {
        this.addresses = Collections.unmodifiableList(new ArrayList<>(addresses));
        this.timeout = timeout;
    }

    public List<SocketAddress> getNodeAddresses() {
        return addresses;
    }

    public int getTimeout() {
        return timeout;
    }
}
