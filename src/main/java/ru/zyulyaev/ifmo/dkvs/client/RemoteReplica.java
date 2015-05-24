package ru.zyulyaev.ifmo.dkvs.client;

import ru.zyulyaev.ifmo.dkvs.shared.Remote;
import ru.zyulyaev.ifmo.dkvs.shared.RemoteMessageProcessor;

/**
 * Created by nikita on 23.05.15.
 */
public class RemoteReplica extends Remote {
    private final int index;
    private final RemoteMessageProcessor<RemoteReplica> messageProcessor;

    public RemoteReplica(int index, RemoteMessageProcessor<RemoteReplica> messageProcessor) {
        this.index = index;
        this.messageProcessor = messageProcessor;
    }

    public int getIndex() {
        return index;
    }

    @Override
    protected void processIncomingMessage(String message) {
        messageProcessor.process(message, this);
    }
}
