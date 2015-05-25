package ru.zyulyaev.ifmo.dkvs;

import ru.zyulyaev.ifmo.dkvs.server.Node;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nikita on 23.05.15.
 */
public class Cluster implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(Cluster.class.getName());

    private final DkvsConfig config;
    private final NodeThread[] threads;

    public Cluster(DkvsConfig config) throws InterruptedException {
        int nodesCount = config.getNodeAddresses().size();
        this.config = config;
        this.threads = new NodeThread[nodesCount];
        for (int i = 0; i < nodesCount; ++i) {
            threads[i] = new NodeThread(i, config);
            threads[i].start();
        }
    }

    @Override
    public void close() throws Exception {
        for (NodeThread thread : threads) {
            thread.stopped = true;
            thread.node.stop();
        }
        for (Thread thread : threads) {
            thread.join(5000);
        }
    }

    private static class NodeThread extends Thread {
        private final int index;
        private final DkvsConfig config;
        private volatile boolean stopped = false;
        private volatile Node node;

        private NodeThread(int index, DkvsConfig config) {
            this.index = index;
            this.config = config;
        }

        @Override
        public void run() {
            boolean crashed = false;
            while (!stopped) {
                try {
                    node = new Node(index, config);
                    node.start(crashed);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Node crashed", e);
                }
                crashed = true;
            }
        }
    }
}
