package ru.zyulyaev.ifmo.dkvs.client;

import ru.zyulyaev.ifmo.dkvs.DkvsConfig;
import ru.zyulyaev.ifmo.dkvs.MessageFormatter;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.request.DeleteRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.GetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.SetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.response.DeletedResponseMessage;
import ru.zyulyaev.ifmo.dkvs.message.response.NotFoundResponseMessage;
import ru.zyulyaev.ifmo.dkvs.message.response.StoredResponseMessage;
import ru.zyulyaev.ifmo.dkvs.message.response.ValueResponseMessage;
import ru.zyulyaev.ifmo.dkvs.shared.BoundChannel;
import ru.zyulyaev.ifmo.dkvs.shared.SocketChannelProcessor;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by nikita on 23.05.15.
 */
public class Client implements Closeable {
    private static final MessageFormatter formatter = MessageFormatter.builder()
            .register(DeletedResponseMessage.class)
            .register(NotFoundResponseMessage.class)
            .register(StoredResponseMessage.class)
            .register(ValueResponseMessage.class)
            .build();

    private final int clientId;
    private final DkvsConfig config;
    private final List<RemoteReplica> replicas;
    private final Selector selector;
    private int viewNumber = 0;
    private int requestId = 0;
    private Message lastMessage;

    public Client(int clientId, DkvsConfig config) throws IOException {
        this.clientId = clientId;
        this.config = config;
        this.replicas = IntStream.range(0, getNodesCount())
                .mapToObj(i -> new RemoteReplica(i, this::processMessage))
                .collect(Collectors.toList());
        selector = Selector.open();
        for (int i = 0; i < getNodesCount(); ++i) {
            SocketAddress address = this.config.getNodeAddresses().get(i);
            replicas.get(i).setOutbound(new BoundChannel(address, selector, () -> {}));
        }
    }

    private int getNodesCount() {
        return config.getNodeAddresses().size();
    }

    public int getPrimaryIndex() {
        return viewNumber % getNodesCount();
    }

    private Message sendPrimaryAndWait(Message message) throws IOException {
        replicas.get(getPrimaryIndex()).sendMessage(message);
        return waitMessage();
    }

    private Message sendAllAndWait(Message message) throws IOException {
        replicas.forEach(r -> r.sendMessage(message));
        return waitMessage();
    }

    private Message sendAndWait(Message request) throws IOException {
        Message response = sendPrimaryAndWait(request);
        while (response == null) {
            response = sendAllAndWait(request);
        }
        return response;
    }

    public void set(String key, String value) throws IOException {
        sendAndWait(new SetRequestMessage(clientId, requestId++, key, value));
    }

    public String get(String key) throws IOException {
        Message response = sendAndWait(new GetRequestMessage(clientId, requestId++, key));
        if (response instanceof ValueResponseMessage)
            return ((ValueResponseMessage) response).getValue();
        else
            return null;
    }

    public boolean delete(String key) throws IOException {
        return sendAndWait(new DeleteRequestMessage(clientId, requestId++, key)) instanceof DeletedResponseMessage;
    }

    private Message waitMessage() throws IOException {
        lastMessage = null;
        while (selector.select(config.getTimeout()) != 0) {
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                SocketChannelProcessor processor = (SocketChannelProcessor) key.attachment();
                processor.process(key);
            }
            keys.clear();
            if (lastMessage != null)
                return lastMessage;
        }
        return lastMessage;
    }

    private void processMessage(String message, RemoteReplica replica) {
        lastMessage = formatter.parse(message);
    }

    @Override
    public void close() throws IOException {
        IOException rethrow = null;
        for (RemoteReplica replica : replicas) {
            try {
                replica.close();
            } catch (IOException ex) {
                if (rethrow == null)
                    rethrow = ex;
                else
                    rethrow.addSuppressed(ex);
            }
        }
        if (rethrow != null)
            throw rethrow;
    }
}
