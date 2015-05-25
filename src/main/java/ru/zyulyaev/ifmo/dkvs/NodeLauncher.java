package ru.zyulyaev.ifmo.dkvs;

import ru.zyulyaev.ifmo.dkvs.server.Node;

import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by nikita on 16.05.15.
 */
public class NodeLauncher {
    private static final Logger logger = Logger.getLogger(NodeLauncher.class.getName());
    public static final String DEFAULT_PROPERTIES = "dkvs.properties";
    public static final int DEFAULT_TIMEOUT = 10000;
    public static final String NODE_KEY_PREFIX = "node.";
    public static final String TIMEOUT_KEY = "timeout";

    public static void main(String[] args) {
        int index = Integer.parseInt(args[0]);
        try {
            new Node(index, loadConfig()).start(false);
        } catch (Exception ex) {
            logger.severe("Bad config " + ex);
        }
    }

    private static DkvsConfig loadConfig() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(DEFAULT_PROPERTIES));
        int timeout = Integer.parseInt(properties.getProperty(TIMEOUT_KEY, Integer.toString(DEFAULT_TIMEOUT)));
        Map<Integer, InetSocketAddress> addresses = new TreeMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!key.startsWith(NODE_KEY_PREFIX))
                continue;
            int index = Integer.parseInt(key.substring(NODE_KEY_PREFIX.length()));
            String[] parts = value.split(":");
            InetSocketAddress address = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
            addresses.put(index, address);
        }
        return new DkvsConfig(addresses.values(), timeout);
    }
}
