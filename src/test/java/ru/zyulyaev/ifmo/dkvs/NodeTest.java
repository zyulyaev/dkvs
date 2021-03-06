package ru.zyulyaev.ifmo.dkvs;

import org.junit.Assert;
import org.junit.Test;
import ru.zyulyaev.ifmo.dkvs.client.Client;

import java.net.InetSocketAddress;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by nikita on 17.05.15.
 */
public class NodeTest {
    private static final int FIRST_PORT = 2000;
    private static final int NODE_COUNT = 3;
    private static final int TIMEOUT = 5000;

    private static final DkvsConfig CONFIG = new DkvsConfig(
            IntStream.range(FIRST_PORT, FIRST_PORT + NODE_COUNT)
                    .mapToObj(InetSocketAddress::new)
                    .collect(Collectors.toList()),
            TIMEOUT
    );

    @Test
    public void testSimple() throws Exception {
        try (Cluster cluster = new Cluster(CONFIG);
             Client client = new Client(0, CONFIG)) {
            client.set("foo", "bar");
            Assert.assertEquals("bar", client.get("foo"));
            Assert.assertTrue(client.delete("foo"));
            Assert.assertNull(client.get("foo"));
        }
    }

    @Test
    public void testPrimaryAsleep() throws Exception {
        try (Cluster cluster = new Cluster(CONFIG);
             Client client = new Client(0, CONFIG)) {
            client.set("foo", "bar");
            Assert.assertEquals("bar", client.get("foo"));
            Assert.assertTrue(client.delete("foo"));
            Assert.assertNull(client.get("foo"));
            client.sendSleep(0, TIMEOUT + 500);
            client.set("bar", "foo");
            Assert.assertEquals("foo", client.get("bar"));
        }
    }

    @Test
    public void testPrimaryDead() throws Exception {
        try (Cluster cluster = new Cluster(CONFIG);
             Client client = new Client(0, CONFIG)) {
            client.set("foo", "bar");
            Assert.assertEquals("bar", client.get("foo"));
            Assert.assertTrue(client.delete("foo"));
            Assert.assertNull(client.get("foo"));
            client.sendDie(0);
            client.set("bar", "foo");
            Assert.assertEquals("foo", client.get("bar"));
        }
    }
}
