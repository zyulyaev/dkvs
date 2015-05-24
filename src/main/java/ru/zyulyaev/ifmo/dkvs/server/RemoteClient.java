package ru.zyulyaev.ifmo.dkvs.server;

import ru.zyulyaev.ifmo.dkvs.shared.Remote;
import ru.zyulyaev.ifmo.dkvs.shared.RemoteMessageProcessor;

/**
 * Created by nikita on 22.05.15.
 */
public class RemoteClient extends Remote {
    private final RemoteMessageProcessor<? super RemoteClient> messageProcessor;

    public RemoteClient(RemoteMessageProcessor<? super RemoteClient> messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    @Override
    protected void processIncomingMessage(String message) {
        messageProcessor.process(message, this);
    }
}
