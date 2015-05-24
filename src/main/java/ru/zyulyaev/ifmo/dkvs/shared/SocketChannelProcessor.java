package ru.zyulyaev.ifmo.dkvs.shared;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by nikita on 22.05.15.
 */
public interface SocketChannelProcessor {
    void process(SelectionKey key) throws IOException;
}
