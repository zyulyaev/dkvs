package ru.zyulyaev.ifmo.dkvs.server;

import ru.zyulyaev.ifmo.dkvs.DkvsConfig;
import ru.zyulyaev.ifmo.dkvs.MessageRouter;
import ru.zyulyaev.ifmo.dkvs.message.Message;
import ru.zyulyaev.ifmo.dkvs.message.normal.NodeMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.DeleteRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.GetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.RequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.request.SetRequestMessage;
import ru.zyulyaev.ifmo.dkvs.message.response.DeletedResponseMessage;
import ru.zyulyaev.ifmo.dkvs.message.response.NotFoundResponseMessage;
import ru.zyulyaev.ifmo.dkvs.message.response.StoredResponseMessage;
import ru.zyulyaev.ifmo.dkvs.message.response.ValueResponseMessage;
import ru.zyulyaev.ifmo.dkvs.server.workflow.NormalPrimaryWorkflow;
import ru.zyulyaev.ifmo.dkvs.server.workflow.NormalReplicaWorkflow;
import ru.zyulyaev.ifmo.dkvs.server.workflow.Workflow;
import ru.zyulyaev.ifmo.dkvs.server.workflow.WorkflowContext;
import ru.zyulyaev.ifmo.dkvs.shared.BoundChannel;
import ru.zyulyaev.ifmo.dkvs.shared.SocketChannelProcessor;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by nikita on 16.05.15.
 */
public class Node {
    private static final Logger logger = Logger.getLogger(Node.class.getName());
    private static final int TICK_TIMEOUT = 1000;

    private final Map<String, String> storage = new HashMap<>();
    private final DkvsConfig config;
    private final int nodeIndex;
    private final List<RemoteNode> nodes;
    private final Set<Integer> disconnectedNodes = new HashSet<>();
    private final Map<Integer, ClientTableEntry> clientTable = new HashMap<>();
    private final AtomicReference<NodeStatus> status = new AtomicReference<>(NodeStatus.CREATED);
    private final RequestLog log = new RequestLog();
    private Workflow currentWorkflow;
    private int viewNumber = 0;
    private int opNumber = 0;
    private int commitNumber = 0;

    private final MessageRouter<Void> applyRequestRouter = MessageRouter.<Void>builder()
            .add(GetRequestMessage.class, this::applyGet)
            .add(SetRequestMessage.class, this::applySet)
            .add(DeleteRequestMessage.class, this::applyDelete)
            .build();

    public Node(int nodeIndex, DkvsConfig config) {
        this.nodeIndex = nodeIndex;
        this.config = config;
        this.nodes = IntStream.range(0, getNodeCount())
                .mapToObj(i -> new RemoteNode(i, this::processNodeMessage))
                .collect(Collectors.toList());
    }

    public int getNodeCount() {
        return config.getNodeAddresses().size();
    }

    public SocketAddress getAddress() {
        return config.getNodeAddresses().get(nodeIndex);
    }

    public NodeStatus getStatus() {
        return status.get();
    }

    private int getPrimaryIndex() {
        return viewNumber % getNodeCount();
    }

    private boolean isPrimaryNode() {
        return getPrimaryIndex() == nodeIndex;
    }

    public void start(boolean recovery) throws IOException {
        if (!status.compareAndSet(NodeStatus.CREATED, NodeStatus.INITIALIZING))
            throw new IllegalStateException("Node #" + nodeIndex + " already has status " + getStatus());

        SocketAddress address = getAddress();
        logger.info("Initializing node #" + nodeIndex + " at " + address);
        try (Selector selector = Selector.open();
             ServerSocketChannel serverSocket = initServerSocket(selector, address)) {
            initNodes(selector);
            initWorkflow();

            if (!status.compareAndSet(NodeStatus.INITIALIZING, currentWorkflow.getStatus())) {
                throw new IllegalStateException("Something when wrong");
            }

            if (recovery)
                currentWorkflow.startRecovery();

            while (true) {
                for (int disconnected : disconnectedNodes)
                    connectNode(disconnected, selector);
                disconnectedNodes.clear();
                int selected = selector.select(TICK_TIMEOUT);
                if (status.get() == NodeStatus.STOPPED || Thread.interrupted())
                    break;
                if (selected != 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    for (SelectionKey key : keys) {
                        SocketChannelProcessor processor = (SocketChannelProcessor) key.attachment();
                        processor.process(key);
                    }
                    keys.clear();
                }
                tick();
            }
        } finally {
            for (RemoteNode node : nodes)
                if (node.getIndex() != nodeIndex)
                    node.close();
            for (ClientTableEntry entry : clientTable.values())
                if (entry.getRemoteClient() != null)
                    entry.getRemoteClient().close();
        }
    }

    private void tick() {
        currentWorkflow.tick();
    }

    private void initWorkflow() {
        NodeWorkflowContext context = new NodeWorkflowContext();
        if (isPrimaryNode()) {
            currentWorkflow = new NormalPrimaryWorkflow(context);
        } else {
            currentWorkflow = new NormalReplicaWorkflow(context);
        }
    }

    private void initNodes(Selector selector) throws IOException {
        for (int i = 0; i < nodes.size(); ++i)
            if (i != nodeIndex) {
                connectNode(i, selector);
            }
    }

    private void connectNode(int index, Selector selector) throws IOException {
        RemoteNode node = nodes.get(index);
        node.setOutbound(new BoundChannel(
                config.getNodeAddresses().get(index),
                selector,
                () -> disconnectedNodes.add(index)
        ));
        node.sendMessage(new NodeMessage(nodeIndex));
    }

    private ServerSocketChannel initServerSocket(Selector selector, SocketAddress address) throws IOException {
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(address);
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT, (SocketChannelProcessor) this::acceptClient);
        return serverSocket;
    }

    private void acceptClient(SelectionKey key) throws IOException {
        RemoteClient client = new RemoteClient(this::processClientMessage);
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = channel.accept();
        client.setOutbound(new BoundChannel(clientChannel, key.selector(), () -> {
        }));
        logger.info("Accepting connection from " + clientChannel.getRemoteAddress());
    }

    public void stop() {
        NodeStatus current;
        do {
            current = status.get();
            if (current == NodeStatus.CREATED)
                throw new IllegalStateException("Node #" + nodeIndex + " not started yet");
        } while (!status.compareAndSet(current, NodeStatus.STOPPED));
    }

    private ClientTableEntry getOrCreateClientEntry(int clientId) {
        return clientTable.computeIfAbsent(clientId, ClientTableEntry::new);
    }

    public void respondClient(RequestMessage request, Message response) {
        int clientId = request.getClientId();
        int requestId = request.getRequestId();
        ClientTableEntry entry = getOrCreateClientEntry(clientId);
        if (entry.getLastRequestId() == requestId)
            entry.setLastResponse(response);
        if (isPrimaryNode() && entry.getRemoteClient() != null) {
            entry.getRemoteClient().sendMessage(response);
        }
    }

    private void processClientMessage(String message, RemoteClient origin) {
        currentWorkflow.processClientMessage(message, origin);
    }

    private void processNodeMessage(String message, RemoteNode origin) {
        currentWorkflow.processNodeMessage(message, origin);
    }

    private void applyGet(GetRequestMessage getMessage, Void ctx) {
        int requestId = getMessage.getRequestId();
        String key = getMessage.getKey();
        String value = storage.get(key);
        if (value == null) {
            respondClient(getMessage, new NotFoundResponseMessage(requestId, viewNumber));
        } else {
            respondClient(getMessage, new ValueResponseMessage(requestId, viewNumber, key, value));
        }
    }

    private void applySet(SetRequestMessage setMessage, Void ctx) {
        storage.put(setMessage.getKey(), setMessage.getValue());
        respondClient(setMessage, new StoredResponseMessage(setMessage.getRequestId(), viewNumber));
    }

    private void applyDelete(DeleteRequestMessage deleteMessage, Void ctx) {
        int requestId = deleteMessage.getRequestId();
        boolean existed = storage.remove(deleteMessage.getKey()) != null;
        if (existed) {
            respondClient(deleteMessage, new DeletedResponseMessage(requestId, viewNumber));
        } else {
            respondClient(deleteMessage, new NotFoundResponseMessage(requestId, viewNumber));
        }
    }

    private class NodeWorkflowContext implements WorkflowContext {
        @Override
        public int getNodeIndex() {
            return nodeIndex;
        }

        @Override
        public int getNodeCount() {
            return Node.this.getNodeCount();
        }

        @Override
        public boolean isPrimaryNode() {
            return Node.this.isPrimaryNode();
        }

        @Override
        public int getViewNumber() {
            return viewNumber;
        }

        @Override
        public void setViewNumber(int viewNumber) {
            Node.this.viewNumber = viewNumber;
        }

        @Override
        public int getOpNumber() {
            return opNumber;
        }

        @Override
        public void setOpNumber(int opNumber) {
            Node.this.opNumber = opNumber;
        }

        @Override
        public int getCommitNumber() {
            return commitNumber;
        }

        @Override
        public RequestLog getLog() {
            return log;
        }

        @Override
        public ClientTableEntry getOrCreateClientEntry(int clientId) {
            return Node.this.getOrCreateClientEntry(clientId);
        }

        @Override
        public void identifyAsNode(RemoteClient client, int nodeId) {
            RemoteNode node = nodes.get(nodeId);
            node.setInbound(client.getOutbound());
        }

        @Override
        public void sendMessageToNode(int nodeId, Message message) {
            nodes.get(nodeId).sendMessage(message);
        }

        @Override
        public void sendMessageToOtherNodes(Message message) {
            nodes.stream()
                    .filter(node -> node.getIndex() != nodeIndex)
                    .forEach(node -> node.sendMessage(message));
        }

        @Override
        public void sendMessageToPrimaryNode(Message message) {
            nodes.get(getPrimaryIndex()).sendMessage(message);
        }

        @Override
        public void switchWorkflow(Workflow workflow) {
            currentWorkflow = workflow;
            status.set(workflow.getStatus());
        }

        @Override
        public void commit() {
            applyRequestRouter.dispatch(log.get(commitNumber++), null);
        }

        @Override
        public int getIdleTimeout() {
            return config.getTimeout();
        }

        @Override
        public void die() {
            status.set(NodeStatus.STOPPED);
        }
    }
}
