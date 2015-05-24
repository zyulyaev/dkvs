package ru.zyulyaev.ifmo.dkvs;

import ru.zyulyaev.ifmo.dkvs.server.Node;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Created by nikita on 23.05.15.
 */
public class Cluster implements AutoCloseable {
    private final DkvsConfig config;
    private final Node[] nodes;
    private final Thread[] threads;

    public Cluster(DkvsConfig config) throws InterruptedException {
        int nodesCount = config.getNodeAddresses().size();
        this.config = config;
        this.nodes = new Node[nodesCount];
        this.threads = new Thread[nodesCount];
        for (int i = 0; i < nodesCount; ++i) {
            Node node = nodes[i] = new Node(i, config);
            threads[i] = new Thread(() -> {
                try {
                    node.start();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            threads[i].start();
        }
    }

    @Override
    public void close() throws Exception {
        for (Node node : nodes)
            node.stop();
        for (Thread thread : threads) {
            thread.interrupt();
        }
        for (Thread thread : threads) {
            thread.join(1000);
        }
    }
}
