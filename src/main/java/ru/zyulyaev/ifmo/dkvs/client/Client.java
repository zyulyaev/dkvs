package ru.zyulyaev.ifmo.dkvs.client;

import ru.zyulyaev.ifmo.dkvs.DkvsConfig;
import ru.zyulyaev.ifmo.dkvs.MessageFormatter;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.debug.DieMessage;
import ru.zyulyaev.ifmo.dkvs.message.debug.SleepMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.DeleteRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.GetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.RequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.SetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.response.*;
import ru.zyulyaev.ifmo.dkvs.shared.BoundChannel;
import ru.zyulyaev.ifmo.dkvs.shared.SocketChannelProcessor;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
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

    private final Set<Integer> disconnected = new HashSet<>();
    private final int clientId;
    private final DkvsConfig config;
    private final List<RemoteReplica> replicas;
    private final Selector selector;
    private int viewNumber = 0;
    private int nextRequestId = 0;
    private ResponseMessage lastMessage;

    public Client(int clientId, DkvsConfig config) throws IOException {
        this.clientId = clientId;
        this.config = config;
        this.replicas = IntStream.range(0, getNodesCount())
                .mapToObj(i -> new RemoteReplica(i, this::processMessage))
                .collect(Collectors.toList());
        selector = Selector.open();
        for (int i = 0; i < getNodesCount(); ++i) {
            connectNode(i);
        }
    }

    private void connectNode(int nodeIndex) throws IOException {
        SocketAddress address = config.getNodeAddresses().get(nodeIndex);
        replicas.get(nodeIndex).setOutbound(new BoundChannel(address, selector, () -> disconnected.add(nodeIndex)));
    }

    private int getNodesCount() {
        return config.getNodeAddresses().size();
    }

    public int getPrimaryIndex() {
        return viewNumber % getNodesCount();
    }

    private ResponseMessage sendPrimaryAndWait(RequestMessage message) throws IOException {
        replicas.get(getPrimaryIndex()).sendMessage(message);
        return waitMessage(message.getRequestId());
    }

    private ResponseMessage sendAllAndWait(RequestMessage message) throws IOException {
        replicas.forEach(r -> r.sendMessage(message));
        return waitMessage(message.getRequestId());
    }

    private void sendOne(int nodeIndex, Message message) {
        replicas.get(nodeIndex).sendMessage(message);
    }

    private ResponseMessage sendAndWait(RequestMessage request) throws IOException {
        ResponseMessage response = sendPrimaryAndWait(request);
        while (response == null || response.getRequestId() != request.getRequestId()) {
            response = sendAllAndWait(request);
        }
        viewNumber = response.getViewNumber();
        return response;
    }

    public void set(String key, String value) throws IOException {
        sendAndWait(new SetRequestMessage(clientId, nextRequestId++, key, value));
    }

    public String get(String key) throws IOException {
        Message response = sendAndWait(new GetRequestMessage(clientId, nextRequestId++, key));
        if (response instanceof ValueResponseMessage)
            return ((ValueResponseMessage) response).getValue();
        else
            return null;
    }

    public boolean delete(String key) throws IOException {
        return sendAndWait(new DeleteRequestMessage(clientId, nextRequestId++, key)) instanceof DeletedResponseMessage;
    }

    public void sendSleep(int nodeIndex, int timeout) {
        sendOne(nodeIndex, new SleepMessage(timeout));
    }

    public void sendDie(int nodeIndex) {
        sendOne(nodeIndex, new DieMessage());
    }

    private ResponseMessage waitMessage(int requestId) throws IOException {
        for (int nodeIndex : disconnected)
            connectNode(nodeIndex);
        disconnected.clear();
        while (selector.select(config.getTimeout()) != 0) {
            Set<SelectionKey> keys = selector.selectedKeys();
            for (SelectionKey key : keys) {
                SocketChannelProcessor processor = (SocketChannelProcessor) key.attachment();
                processor.process(key);
            }
            keys.clear();
            if (lastMessage != null && lastMessage.getRequestId() == requestId)
                return lastMessage;
        }
        return lastMessage;
    }

    private void processMessage(String message, RemoteReplica replica) {
        ResponseMessage incoming = (ResponseMessage) formatter.parse(message);
        if (lastMessage == null || incoming.getRequestId() > lastMessage.getRequestId())
            lastMessage = incoming;
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
